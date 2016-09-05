/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.ai.platform.modules.sys.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.platform.common.beanvalidator.BeanValidators;
import com.ai.platform.common.config.Global;
import com.ai.platform.common.persistence.Page;
import com.ai.platform.common.utils.StringUtils;
import com.ai.platform.common.web.BaseController;
import com.ai.platform.modules.sys.entity.Area;
import com.ai.platform.modules.sys.entity.GnArea;
import com.ai.platform.modules.sys.entity.Office;
import com.ai.platform.modules.sys.entity.User;
import com.ai.platform.modules.sys.service.OfficeService;
import com.ai.platform.modules.sys.utils.DictUtils;
import com.ai.platform.modules.sys.utils.UserUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 部门Controller
 * @author ThinkGem
 * @version 2013-5-15
 */
@Controller
@RequestMapping(value = "${adminPath}/sys/office")
public class OfficeController extends BaseController {

	@Autowired
	private OfficeService officeService;
	
	@ModelAttribute("office")
	public Office get(@RequestParam(required=false) String id) {
		if (StringUtils.isNotBlank(id)){
			return officeService.get(id);
		}else{
			return new Office();
		}
	}

	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = {""})
	public String index(Office office, Model model) {
        model.addAttribute("list", officeService.findList(false));
 
		return "modules/sys/officeList";
	}

	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = {"treePage"})
	public String tree(Office office, Model model) {
		return "modules/mgmtsys/officetree";
	}
	
	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = {"list"})
	public String list(Office office, Model model) {
        model.addAttribute("list", officeService.findList(office));
		return "modules/sys/officeList";
	}
	
	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = {"page"})
	public String page(Office office, HttpServletRequest request, HttpServletResponse response,Model model) {
		 if(office!=null && office.getParent()!=null){
			 request.setAttribute("upName", office.getParent().getName());
			 request.setAttribute("id", office.getParent().getId());
		 }
		
		//翻译部门类别、等级
		Page<Office> pge = officeService.findPage(new Page<Office>(request, response), office);
		List<Office> list = pge.getList();
		for(Office o : list){
			String type = DictUtils.getDictLabel(o.getType(), "sys_office_type", "未知");
			String grade = DictUtils.getDictLabel(o.getGrade(), "sys_office_grade", "未知");
			o.setGrade(grade);
			o.setType(type);
		}
		model.addAttribute("page", pge);
		return "modules/mgmtsys/iotofficeList";
	}
	/**
	 * 新增部门
	 * @param office
	 * @param model
	 * @return
	 * @author zhanglh
	 * @ApiCode
	 */
	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = "add")
	public String add(Office office, Model model) {
		User user = UserUtils.getUser();
		if ((office.getParent()==null || office.getParent().getId()==null)&& user!=null){
			office.setParent(user.getOffice());
		}
		office.setParent(officeService.get(office.getParent().getId()));
		if (office.getArea()==null && user!=null){
			office.setArea(user.getOffice().getArea());
		}
		// 自动获取排序号
		if (StringUtils.isBlank(office.getId())&&office.getParent()!=null){
			int size = 0;
			List<Office> list = officeService.findAll();
			for (int i=0; i<list.size(); i++){
				Office e = list.get(i);
				if (e.getParent()!=null && e.getParent().getId()!=null
						&& e.getParent().getId().equals(office.getParent().getId())){
					size++;
				}
			}
			office.setCode(office.getParent().getCode() + StringUtils.leftPad(String.valueOf(size > 0 ? size+1 : 1), 3, "0"));
		}
		model.addAttribute("office", office);
		return "modules/mgmtsys/officeForm";
	}
	@RequiresPermissions("sys:office:view")
	@RequestMapping(value = "form")
	public String form(Office office, Model model) {
		User user = UserUtils.getUser();
		if(user!=null){
			if (office.getParent()==null || office.getParent().getId()==null){
				office.setParent(user.getOffice());
			}
		}
		office.setParent(officeService.get(office.getParent().getId()));
		if (office.getArea()==null){
			office.setArea(user.getOffice().getArea());
		}
		// 自动获取排序号
		if (StringUtils.isBlank(office.getId())&&office.getParent()!=null){
			int size = 0;
			List<Office> list = officeService.findAll();
			for (int i=0; i<list.size(); i++){
				Office e = list.get(i);
				if (e.getParent()!=null && e.getParent().getId()!=null
						&& e.getParent().getId().equals(office.getParent().getId())){
					size++;
				}
			}
			office.setCode(office.getParent().getCode() + StringUtils.leftPad(String.valueOf(size > 0 ? size+1 : 1), 3, "0"));
		}
		model.addAttribute("office", office);
		return "modules/sys/officeForm";
	}
	
	@RequiresPermissions("sys:office:edit")
	@RequestMapping(value = "save")
	public String save(Office office, Model model, RedirectAttributes redirectAttributes) {
		if(Global.isDemoMode()){
			addMessage(redirectAttributes, "演示模式，不允许操作！");
			return "redirect:" + adminPath + "/sys/office/";
		}
		if (!beanValidator(model, office)){
			return form(office, model);
		}
/*		if (!"true".equals(checkName(office.getOldName(),office.getName()))){
			addMessage(model, "保存部门'" + office.getName() + "'失败, 部门名称已存在");
			return form(office, model);
		}*/
		if (!"true".equals(checkCode(office.getOldCode(),office.getCode()))){
			addMessage(model, "保存部门'" + office.getCode() + "'失败, 部门编码已存在");
			return form(office, model);
		}
		officeService.save(office);
		
		if(office.getChildDeptList()!=null){
			Office childOffice = null;
			for(String id : office.getChildDeptList()){
				childOffice = new Office();
				childOffice.setName(DictUtils.getDictLabel(id, "sys_office_common", "未知"));
				childOffice.setParent(office);
				childOffice.setArea(office.getArea());
				childOffice.setType("2");
				childOffice.setGrade(String.valueOf(Integer.valueOf(office.getGrade())+1));
				childOffice.setUseable(Global.YES);
				officeService.save(childOffice);
			}
		}
		
		addMessage(redirectAttributes, "保存部门'" + office.getName() + "'成功");
		//String id = "0".equals(office.getParentId()) ? "" : office.getParentId();
		//return "redirect:" + adminPath + "/sys/office/list?id="+id+"&parentIds="+office.getParentIds();
		return "redirect:" + adminPath + "/sys/office/page";
	}
	
	/**
	 * 导入部门信息数据
	 * 
	 * @param file
	 * @param redirectAttributes
	 * @return
	 */
	@RequiresPermissions("sys:office:edit")
	@RequestMapping(value = "import", method = RequestMethod.POST)
	public String importFile(MultipartFile file, RedirectAttributes redirectAttributes) {
		if (Global.isDemoMode()) {
			addMessage(redirectAttributes, "演示模式，不允许操作！");
			return "redirect:" + adminPath + "/sys/office/page?repage";
		}
		try {
			// 验证导入文件是否合法
			String fileName = file.getOriginalFilename();
			if (StringUtils.isBlank(fileName)) {
				throw new RuntimeException("导入文档为空!");
			} else if (!fileName.toLowerCase().endsWith(".txt")) {
				throw new RuntimeException("文档格式不正确!");
			}
			int successNum = 0;
			int failureNum = 0;
			int alldataNum = 0;
			StringBuilder failureMsg = new StringBuilder();
			InputStream is;
			is = file.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String lineContent;
			while ((lineContent = br.readLine()) != null) {
				if (lineContent.startsWith("#CODE"))
					continue;
				alldataNum++;
				String[] userInfo = lineContent.split("\\\\t");
				if(userInfo.length !=6)
					throw new RuntimeException("文档格式不正确!");
				//封装导入用户信息
				Office office = setOfficeInfo(userInfo);
				try {
					if ("true".equals(checkCode("", office.getCode()))) {
						office.setType("1");
						office.setGrade("1");
						office.setTenantId("changhong_d");
						office.setDelFlag("0");
						office.setZipCode("");
						office.setRemarks("");
						office.setPhone("");
						office.setEmail("");
						office.setAddress("");
						office.setFax("");
//						BeanValidators.validateWithException(validator, office);
						if("".equals(office.getCode()) || office.getCode() ==null){
							failureMsg.append("<br/> 第" + alldataNum + "条数据机构编码，不能为空; ");
							failureNum++;
							continue;
						}
						if("".equals(office.getName()) || office.getName() ==null){
							failureMsg.append("<br/> 第" + alldataNum + "条数据机构名称，不能为空; ");
							failureNum++;
							continue;
						}
						if("".equals(office.getParentIds()) || office.getParentIds() ==null){
							failureMsg.append("<br/> 第" + alldataNum + "条数据上级编码，不能为空; ");
							failureNum++;
							continue;
						}
						successNum++;
						officeService.save(office);
					} else {
						failureMsg.append("<br/>编码 " + office.getCode() + " 已存在; ");
						failureNum++;
					}
				} catch (ConstraintViolationException ex) {
					ex.printStackTrace();
					failureMsg.append("<br/>编码 " + office.getCode() + " 导入失败：");
					List<String> messageList = BeanValidators.extractPropertyAndMessageAsList(ex, ": ");
					for (String message : messageList) {
						failureMsg.append(message + "; ");
						failureNum++;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					failureMsg.append("<br/>编码 " + office.getCode() + " 导入失败：" + ex.getMessage());
				}
			}
			//关闭文件流
			br.close();
			isr.close();
			is.close();

			if (failureNum > 0) {
				failureMsg.insert(0, "，失败 " + failureNum + " 条部门信息，导入信息如下：");
			}
			addMessage(redirectAttributes, "已成功导入 " + successNum + " 条部门信息" + failureMsg);
		} catch (Exception e) {
			addMessage(redirectAttributes, "导入部门信息失败！失败信息：" + e.getMessage());
		}
		return "redirect:" + adminPath + "/sys/office/page?repage";
	}
	
	/**
	 * 封装导入用户信息
	 * @param userInfo
	 * @return user
	 */
	private Office setOfficeInfo(String[] officeInfo){
		Office office = new Office();
		office.setCode(officeInfo[0]);
		office.setName(officeInfo[1]);
		office.setParentIds(officeInfo[2]);
		office.setSort(Integer.valueOf(officeInfo[3]));
		office.setMaster(officeInfo[4]);
		Area area = new Area();
		area.setId(officeInfo[5]);
		office.setArea(area);
		return office;
	}
	
	@RequiresPermissions("sys:office:edit")
	@RequestMapping(value = "delete")
	public String delete(Office office, RedirectAttributes redirectAttributes) {
		if(Global.isDemoMode()){
			addMessage(redirectAttributes, "演示模式，不允许操作！");
			return "redirect:" + adminPath + "/sys/office/list";
		}
//		if (Office.isRoot(id)){
//			addMessage(redirectAttributes, "删除部门失败, 不允许删除顶级部门或编号空");
//		}else{
			officeService.delete(office);
			addMessage(redirectAttributes, "删除部门成功");
//		}
		//return "redirect:" + adminPath + "/sys/office/list?id="+office.getParentId()+"&parentIds="+office.getParentIds();
			return "redirect:" + adminPath + "/sys/office/page";
	}
	/**
	 * 验证部门名称是否有效
	 * @param oldName
	 * @param name
	 * @return
	 */
	@RequiresPermissions("sys:office:edit")
	@ResponseBody
	@RequestMapping(value = "checkName")
	public String checkName(String oldName,String name) {
		Office o = new Office();
		o.setName(name);
		List<Office> list = officeService.find(o);
		if (name!=null && name.equals(oldName)) {
			return "true";
		} else if (name!=null &&  CollectionUtil.isEmpty(list)) {
			return "true";
		}
		return "false";
	}
	/**
	 * 验证部门代码是否有效
	 * @param oldName
	 * @param name
	 * @return
	 */
	@RequiresPermissions("sys:office:edit")
	@ResponseBody
	@RequestMapping(value = "checkCode")
	public String checkCode( String oldCode,String code) {
		Office o = new Office();
		o.setCode(code);
		List<Office> list = officeService.find(o);
		if (code!=null && code.equals(oldCode)) {
			return "true";
		} else if (code!=null &&  CollectionUtil.isEmpty(list)) {
			return "true";
		}
		return "false";
	}
	/**
	 * 获取部门JSON数据。
	 * @param extId 排除的ID
	 * @param type	类型（1：公司；2：部门/小组/其它：3：用户）
	 * @param grade 显示级别
	 * @param response
	 * @return
	 */
	@RequiresPermissions("user")
	@ResponseBody
	@RequestMapping(value = "treeData")
	public List<Map<String, Object>> treeData(@RequestParam(required=false) String extId, @RequestParam(required=false) String type,
			@RequestParam(required=false) Long grade, @RequestParam(required=false) Boolean isAll, HttpServletResponse response) {
		List<Map<String, Object>> mapList = Lists.newArrayList();
		List<Office> list = officeService.findList(isAll);
		for (int i=0; i<list.size(); i++){
			Office e = list.get(i);
			/*if ((StringUtils.isBlank(extId) || (extId!=null && !extId.equals(e.getId()) && e.getParentIds().indexOf(","+extId+",")==-1))
					&& (type == null || (type != null && (type.equals("1") ? type.equals(e.getType()) : true)))
					&& (grade == null || (grade != null && Integer.parseInt(e.getGrade()) <= grade.intValue()))
					&& Global.YES.equals(e.getUseable())){*/
				Map<String, Object> map = Maps.newHashMap();
				map.put("id", e.getId());
				map.put("pId", e.getParentId());
				map.put("pIds", e.getParentIds());
				map.put("name", e.getName());
				map.put("code", e.getCode());
				if (type != null && "3".equals(type)){
					map.put("isParent", true);
				}
				mapList.add(map);
			//}
		}
		return mapList;
	}
}
