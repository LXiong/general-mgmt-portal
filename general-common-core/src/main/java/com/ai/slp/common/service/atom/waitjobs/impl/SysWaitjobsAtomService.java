package com.ai.slp.common.service.atom.waitjobs.impl;

import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.ai.opt.sdk.components.sequence.factory.SeqClientFactory;
import com.ai.opt.sdk.components.sequence.util.SeqUtil;
import com.ai.opt.sdk.util.DateUtil;
import com.ai.slp.common.constants.VOConstants.WaitjobsStatusConstant;
import com.ai.slp.common.dao.mapper.bo.SysWaitjobs;
import com.ai.slp.common.dao.mapper.bo.SysWaitjobsCriteria;
import com.ai.slp.common.dao.mapper.bo.SysWaitjobsCriteria.Criteria;
import com.ai.slp.common.dao.mapper.factory.MapperFactory;
import com.ai.slp.common.service.atom.waitjobs.ISysWaitjobsAtomService;

@Component
public class SysWaitjobsAtomService implements ISysWaitjobsAtomService{

	@Override
	public String insertWaitjobs(SysWaitjobs waitjobs) {
		String status = waitjobs.getStatus();
		if(StringUtils.isEmpty(status)){
			waitjobs.setStatus(WaitjobsStatusConstant.WAIT);
		}
		Timestamp arriveData = waitjobs.getArriveData();
		if(arriveData == null){
			Timestamp sysDate = DateUtil.getSysDate();
			waitjobs.setArriveData(sysDate);
		}
		String newId = SeqUtil.getNewId("SYS$SYSWAITJOBS$ID").toString();
		waitjobs.setId(newId);
		int insertCount = MapperFactory.getSysWaitjobsMapper().insertSelective(waitjobs);
		if(insertCount>0){
			return newId;
		}else{
			return null;
		}
	}

	@Override
	public int completeWaitjobs(String id) {
		SysWaitjobsCriteria example = new SysWaitjobsCriteria();
		Criteria criteria = example.createCriteria();
		criteria.andIdEqualTo(id);
		SysWaitjobs record = new SysWaitjobs();
		record.setStatus(WaitjobsStatusConstant.END);
		return MapperFactory.getSysWaitjobsMapper().updateByExampleSelective(record , example);
	}

}
