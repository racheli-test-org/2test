package com.dchealth.service.common;

import com.dchealth.entity.common.*;
import com.dchealth.facade.common.BaseFacade;
import org.jboss.logging.annotations.Pos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/6/22.
 */
@Controller
@Produces("application/json")
@Path("role")
public class RoleService {

    @Autowired
    private BaseFacade baseFacade ;

    /**
     * 获取所有角色
     * @return
     */
    @GET
    @Path("list")
    public List<RoleDict> listRole(){
        String hql = "from RoleDict as rd where rd.status='1'" ;//and code !='DOCTOR_ASSISTANT'
        return baseFacade.createQuery(RoleDict.class,hql,new ArrayList<Object>()).getResultList();
    }

    /**
     * 用户审核查询角色
     * @return
     */
    @GET
    @Path("list-audit")
    public List<RoleDict> listAuditRole(){
        String hql = "from RoleDict as rd where rd.status='1' and code !='DOCTOR_ASSISTANT'" ;
        return baseFacade.createQuery(RoleDict.class,hql,new ArrayList<Object>()).getResultList();
    }

    /**
     * 获取角色用户
     * @param roleId
     * @return
     */
    @GET
    @Path("list-role-user")
    public List<YunUsers> listRoleUsers(@QueryParam("roldId") String roleId){
        String hql="select y from YunUsers as y ,RoleVsUser rvu where y.id=rvu.userId and y.status<>'-1' and rvu.roleId='"+roleId+"'" ;
        return baseFacade.createQuery(YunUsers.class,hql,new ArrayList<Object>()).getResultList();
    }


    /**
     * 添加、修改、删除角色
     * 删除为逻辑删除
     * status=1正常、status=-1删除
     * @param roleDict
     * @return
     */
    @POST
    @Path("merge")
    @Transactional
    public Response mergeRoleDict(RoleDict roleDict){
        return Response.status(Response.Status.OK).entity(baseFacade.merge(roleDict)).build();
    }

    /**
     * 添加角色菜单
     * @param menuDicts 菜单对象数组
     * @param roleId 角色ID通过拼接在访问路径后面
     * @return
     * @throws Exception
     */
    @POST
    @Path("add-role-menu")
    @Transactional
    public Response roleMenuAdd(List<MenuDict> menuDicts,@QueryParam("roleId") String roleId) throws Exception {
        if(roleId==null||"".equals(roleId)){
            throw new Exception("传入的角色信息为空！");
        }
        String delHql = "delete RoleVsMenus as rm where rm.roleId='"+roleId+"'" ;
        baseFacade.removeByHql(delHql);
        List<RoleVsMenus> roleVsMenusList = new ArrayList<>();
        for (MenuDict md :menuDicts){
            RoleVsMenus roleVsMenus = new RoleVsMenus();
            roleVsMenus.setRoleId(roleId);
            roleVsMenus.setMenuId(md.getId());
            roleVsMenusList.add(baseFacade.merge(roleVsMenus)) ;
        }

        return Response.status(Response.Status.OK).entity(roleVsMenusList).build();
    }


    /**
     * 添加角色用户
     * @param users
     * @param roleId
     * @return
     * @throws Exception
     */
    @Transactional
    @Path("add-role-user")
    @POST
    public Response addRoleUser(List<YunUsers> users,@QueryParam("roleId") String roleId) throws Exception {
        if(roleId==null||"".equals(roleId)){
            throw new Exception("传入的角色信息为空！");
        }
        String delHql = "delete RoleVsUser as rm where rm.roleId='"+roleId+"'" ;
        baseFacade.removeByHql(delHql);
        List<RoleVsUser> roleVsUserArrayList = new ArrayList<>();
        for (YunUsers user :users){
            RoleVsUser roleVsUser = new RoleVsUser();
            roleVsUser.setRoleId(roleId);
            roleVsUser.setUserId(user.getId());
            roleVsUserArrayList.add(baseFacade.merge(roleVsUser)) ;
        }
        return Response.status(Response.Status.OK).entity(roleVsUserArrayList).build();
    }


    /**
     * 根据角色ID获取角色所有的资源
     * @param roleId
     * @return
     */
    @GET
    @Path("list-role-resource")
    public List<ResourceDict> listRoleResource(@QueryParam("roleId") String roleId){
        String hql = "select re from RoleDict rd ,RoleVsResource rvr ,ResourceDict re where rd.status='1' " +
                "and re.status='1' and rd.id=rvr.roleId" +
                " and re.id=rvr.resourceId and " +
                " rd.id='"+roleId+"'" ;
        List<ResourceDict> resourceDicts = baseFacade.createQuery(ResourceDict.class, hql, new ArrayList<Object>()).getResultList();
        return resourceDicts;
    }

    /**
     * 添加角色对应的资源
     * @param roleId
     * @param resourceDicts
     * @return
     * @throws Exception
     */
    @Transactional
    @Path("add-role-resource")
    @POST
    public Response mergeResource(@QueryParam("roleId")String roleId,List<ResourceDict> resourceDicts) throws Exception {
        String hql ="delete RoleVsResource where roleId='"+roleId+"'";
        baseFacade.removeByHql(hql);
        if("".equals(roleId)||null==roleId){
            throw  new Exception("角色ID为空！");
        }
        for (ResourceDict resourceDict:resourceDicts){
            RoleVsResource roleVsResource = new RoleVsResource();
            roleVsResource.setRoleId(roleId);
            roleVsResource.setResourceId(resourceDict.getId());
            baseFacade.merge(roleVsResource);
        }
        return Response.status(Response.Status.OK).entity(resourceDicts).build();
    }

}
