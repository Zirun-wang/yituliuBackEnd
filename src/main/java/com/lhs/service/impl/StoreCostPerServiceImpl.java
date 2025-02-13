package com.lhs.service.impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lhs.bean.DBPogo.ItemRevise;
import com.lhs.bean.vo.StoreJson;
import com.lhs.bean.DBPogo.StoreCostPer;
import com.lhs.bean.vo.StoreJsonVo;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.ReadJsonUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.common.util.SaveFile;
import com.lhs.dao.StoreCostPerDao;
import com.lhs.service.ItemService;
import com.lhs.service.StoreCostPerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class StoreCostPerServiceImpl implements StoreCostPerService {
	
	@Autowired
	private  StoreCostPerDao storeCostPerDao ;

	@Autowired
	private ItemService itemService ;

	@Autowired
	private StoreCostPerService storeCostPerService ;



	@Value("${frontEnd.backups}")
	private String backupsPath;

	@Value("${frontEnd.path}")
	private  String frontEndFilePath;

	@Override
	public void save(List<StoreCostPer> storeCostPer) {
		if(storeCostPer!=null) {
			storeCostPerDao.saveAll(storeCostPer);
		}else {
			throw new ServiceException(ResultCode.PARAM_IS_BLANK);
		}

	}




	@Override
	public void updateByJsonPerm() {
		List<ItemRevise> allItem =itemService.findAllItemRevise();
		String str = ReadJsonUtil.readJson(frontEndFilePath+"//permStoreData.json");
		List<StoreJson> storeJsons = JSONArray.parseArray(str, StoreJson.class);

		DecimalFormat dfbfb = new DecimalFormat("0.00");

		List<StoreCostPer> list = new ArrayList<>();

		Long count = 1L;


		for (int i = 0; i < Objects.requireNonNull(storeJsons).size(); i++) {
			for (int j = 0; j < allItem.size(); j++) {
				if (storeJsons.get(i).getItemName().equals(allItem.get(j).getItemName())) {
					Double costPer = allItem.get(j).getItemValue() / Double.parseDouble(storeJsons.get(i).getCost());
					StoreCostPer storeCostPer = new StoreCostPer();
					storeCostPer.setId(count);
					count++;
					storeCostPer.setItemId(allItem.get(j).getItemId());
					storeCostPer.setItemName(storeJsons.get(i).getItemName());
					storeCostPer.setStoreType(storeJsons.get(i).getType());
					storeCostPer.setCostPer(Double.valueOf(dfbfb.format(costPer)));
					storeCostPer.setCost(Double.valueOf(storeJsons.get(i).getCost()));
					storeCostPer.setRawCost(storeJsons.get(i).getRawCost());
					storeCostPer.setItemValue(allItem.get(j).getItemValue());
					list.add(storeCostPer);
				}
			}
		}

		storeCostPerService.save(list);

	}

	@Override
	public void updateByJsonAct(MultipartFile file) {
		List<ItemRevise> allItem =itemService.findAllItemRevise();
		HashMap<String, Double> itemValueMap = new HashMap<>();
		for(ItemRevise itemRevise:allItem){
			itemValueMap.put(itemRevise.getItemName(),itemRevise.getItemValue());
		}

		String fileStr  = ReadJsonUtil.readFile(file);
		JSONArray jsonArray = JSONArray.parseArray(fileStr);
		List<Object>   jsonList  = new ArrayList<>();
		if(jsonArray!=null)
		for(Object object:jsonArray){
			Map storeMap = JSONObject.parseObject(object.toString());
			Object o = storeMap.get("actStore");
			List<StoreJsonVo> storeJsonVos = JSONArray.parseArray(o.toString(),StoreJsonVo.class);

			List<StoreJsonVo>  storeJsonVoResult = new ArrayList<>();

			Integer itemArea = storeJsonVos.get(storeJsonVos.size() - 1).getItemArea();
			for(int i=1;i<=itemArea;i++){
				List<StoreJsonVo> storeJsonVoCopy = new ArrayList<>();
				for(StoreJsonVo storeJsonVo:storeJsonVos){
					if(storeJsonVo.getItemArea()==i){
						storeJsonVo.setItemPPR(itemValueMap.get(storeJsonVo.getItemName())*storeJsonVo.getItemQuantity()/storeJsonVo.getItemPrice());
						storeJsonVoCopy.add(storeJsonVo);
					}
				}
				storeJsonVoCopy.sort(Comparator.comparing(StoreJsonVo::getItemPPR).reversed());
				storeJsonVoResult.addAll(storeJsonVoCopy);
			}
			storeMap.put("actStore",storeJsonVoResult);
			jsonList.add(storeMap);
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");// 设置日期格式
		String saveTime = simpleDateFormat.format(new Date());


		Object fileVo = JSONObject.toJSON(jsonList);
		SaveFile.save(backupsPath,"storeAct"+saveTime+".json",fileVo.toString());
		SaveFile.save(frontEndFilePath,"storeAct.json",fileVo.toString());

	}

	@Override
	public String readActStoreJson() {
		return ReadJsonUtil.readJson(frontEndFilePath+"//storeAct.json");
	}

	@Override
	public String readPermStoreJson() {
		return ReadJsonUtil.readJson(frontEndFilePath+"//storePerm.json");
	}


	@Override
	public List<StoreCostPer> findAll(String type) {
		return storeCostPerDao.findByStoreTypeOrderByCostPerDesc(type);
	}







}
