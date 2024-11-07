package com.dchealth.service.common;

import com.dchealth.VO.Page;
import com.dchealth.VO.YunUserVO;
import com.dchealth.entity.common.RoleDict;
import com.dchealth.entity.common.RoleVsUser;
import com.dchealth.entity.rare.YunDiseaseList;
import com.dchealth.entity.rare.YunUserDisease;
import com.dchealth.entity.rare.YunUserDiseaseManager;
import com.dchealth.entity.common.YunUsers;
import com.dchealth.facade.security.MailSendFacade;
import com.dchealth.facade.security.UserFacade;
import com.dchealth.security.PasswordAndSalt;
import com.dchealth.security.SystemPasswordService;
import com.dchealth.util.SmsSendUtil;
import com.dchealth.util.StringUtils;
import com.dchealth.util.UserUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.mortbay.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/6/6.
 */

@Produces("application/json")
@Path("yun-user")
@Controller
public class YunUserService {

    @Autowired
    private UserFacade userFacade ;

    @Autowired
    private MailSendFacade mailSendFacade;

    private Map<String,Integer> mobileMap = new ConcurrentHashMap<String,Integer>();
    private Map<String,Long> timeMap = new ConcurrentHashMap<String,Long>();
    private final String _Mobile = "mobile";

    /**
     * 注册用户信息
     * @param yunUsers
     * @return
     */
    @Path("regist")
    @POST
    public Response registYunUser(@QueryParam("pictureCode") String pictureCode,@QueryParam("veryCode") String veryCode, YunUsers yunUsers,@Context HttpServletRequest request) throws Exception{
        if("true".equals(SmsSendUtil.getStringByKey("openVeryCode"))){
            if(StringUtils.isEmpty(pictureCode)){
                throw new Exception("图形验证码不能为空");
            }
            String sessionPictureCode = request.getSession().getAttribute(request.getSession().getId()+SmsSendUtil.pictureCodeToRegister)==null?"":request.getSession().getAttribute(request.getSession().getId()+SmsSendUtil.pictureCodeToRegister)+"";
            if(!pictureCode.equals(sessionPictureCode)){
                throw new Exception("图形验证码输入错误，请重新输入");
            }
            if(StringUtils.isEmpty(veryCode)){
                throw new Exception("手机验证码不能为空，请重新输入");
            }
            String sessionVeryCode = request==null?"":(String) request.getSession().getAttribute(request.getSession().getId()+SmsSendUtil.register);
            if(StringUtils.isEmpty(sessionVeryCode)){
                throw new Exception("手机验证码已失效，请重新获取");
            }
            if(!veryCode.equals(sessionVeryCode)){
                throw new Exception("手机验证码不正确，请重新输入");
            }
            String mobile = request.getSession().getAttribute(request.getSession().getId()+_Mobile)==null?"":request.getSession().getAttribute(request.getSession().getId()+_Mobile)+"";
            if(!mobile.equals(yunUsers.getMobile())){
                throw new Exception("发送验证码手机号和注册手机号不一致，请重新输入");
            }
        }
        Response response = null;
        try {
            long id = new Date().getTime();
            yunUsers.setId(String.valueOf(id));
            PasswordAndSalt passwordAndSalt = SystemPasswordService.enscriptPassword(yunUsers.getUserId(), yunUsers.getPassword());
            yunUsers.setPassword(passwordAndSalt.getPassword());
            yunUsers.setSalt(passwordAndSalt.getSalt());
            response = userFacade.mergeYunUsers(yunUsers);
        }catch (Exception e){
            String emsg = e.getMessage();
            if(emsg.contains("yun_users_idx_user_id")){
                throw new Exception("登陆名已存在，请重新输入");
            }else if(emsg.contains("yun_users_idx_mobile")){
                throw new Exception("手机号已被注册，请重新输入");
            }else if(emsg.contains("yun_users_idx_email")){
                throw new Exception("邮箱已被注册，请重新输入");
            }else if(emsg.contains("yun_users_idx_practice_qualification_id")){
                throw new Exception("职业资格证书编号已存在，请重新输入");
            }else{
                throw new Exception("注册异常，请重试");
            }
        }
        request.getSession().removeAttribute(request.getSession().getId()+SmsSendUtil.register);
        request.getSession().removeAttribute(request.getSession().getId()+SmsSendUtil.pictureCode);//清除图形验证码
        //用户注册发送邮件给管理员进行审核
        String mailInfo = "您好:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;用户"+yunUsers.getUserName()+"已注册，请及时审核！";
        mailSendFacade.sendMail("新用户注册",SmsSendUtil.getStringByKey("adminEmail"),mailInfo);
        mailSendFacade.sendMessage(yunUsers,SmsSendUtil.getStringByKey("adminEmail"),"新用户注册",mailInfo);//发送站内信给要审核的管理员
        return response;
    }

    /**
     * 更新用户
     * @param yunUsers
     * @return
     */
    @POST
    @Transactional
    @Path("update")
    public Response updateYunUser(YunUsers yunUsers) throws Exception {
        String id = yunUsers.getId();
        if(id==null||"".equals(id)){
            System.out.println(id);
            throw new Exception("获取不到原信息的ID");
        }
        YunUsers dbUsers = userFacade.get(YunUsers.class,id);
        String loginFlags = dbUsers.getLoginFlags();
        YunUsers users = userFacade.merge(yunUsers);
        if(!users.getLoginFlags().equals(loginFlags) && "R".equals(users.getLoginFlags())){//表示审核通过
            String mailInfo = "您好:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;您的账号"+yunUsers.getUserName()+"已通过审核，您可以进行登录了，登录连接<a href=\"http://nrdrs.org\">http://nrdrs.org</a>";
            mailSendFacade.sendMail("用户审核",users.getEmail(),mailInfo);
            mailSendFacade.sendMessageToUser(yunUsers.getId(),"用户审核",mailInfo);
        }
        return Response.status(Response.Status.OK).entity(users).build();
    }

    /**
     * 修改密码
     * @param oldPassword
     * @param newPassowrd
     * @param userId
     * @return
     */
    @POST
    @Transactional
    @Path("change-pwd")
    public Response chagePassword(@QueryParam("oldPassword") String oldPassword, @QueryParam("newPassword") String newPassowrd,
                                  @QueryParam("userId") String userId) throws Exception {
        YunUsers yunUsers = userFacade.getYunUsersByUserId(userId);
        String passwordWithSalt = SystemPasswordService.enscriptPasswordWithSalt(yunUsers.getSalt(), userId, oldPassword);
        String oldDbPassword = yunUsers.getPassword() ;
        if(passwordWithSalt.equals(oldDbPassword)){
            PasswordAndSalt passwordAndSalt = SystemPasswordService.enscriptPassword(userId, newPassowrd);
            yunUsers.setPassword(passwordAndSalt.getPassword());
            yunUsers.setSalt(passwordAndSalt.getSalt());
            Subject subject = SecurityUtils.getSubject();
            subject.logout();
            return Response.status(Response.Status.OK).entity(userFacade.merge(yunUsers)).build();
        }else{
            throw new Exception("原密码错误！");
        }
    }


    /**
     * 重置用户密码
     * @param userId
     * @return
     * @throws Exception
     */
    @POST
    @Transactional
    @Path("rest-pwd")
    public Response restPassword(@QueryParam("userId")String userId) throws Exception {

        YunUsers yunUsers = userFacade.getYunUsersByUserId(userId) ;
        Properties properties = new Properties() ;
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("dchealth.properties");
        properties.load(resourceAsStream);
        String newPassword =properties.getProperty("newPassword");
        if("".equals(newPassword)||null==newPassword){
            newPassword = "123456" ;
        }
        if (yunUsers!=null){
            PasswordAndSalt passwordAndSalt = SystemPasswordService.enscriptPassword(userId, newPassword);
            yunUsers.setPassword(passwordAndSalt.getPassword());
            yunUsers.setSalt(passwordAndSalt.getSalt());
            userFacade.merge(yunUsers);
        }

        return Response.status(Response.Status.OK).entity(yunUsers).build();
    }

    /**
     * 获取当前登录用户
     * @return
     * @throws Exception
     */
    @GET
    @Path("current-user")
    public YunUsers getCurrentUser() throws Exception {
        YunUsers yunUsers = UserUtils.getYunUsers();
        return yunUsers;
    }


    /**
     * 获取用户研究疾病、管理疾病信息
     * @param userId
     * @return
     */
    @GET
    @Path("get-user-disease-info")
    public YunUserVO getYunUserDiseaseInfo(@QueryParam("userId") String userId) throws Exception {
        YunUserVO yunUserVO = new YunUserVO() ;
        YunUsers yunUsers = userFacade.getYunUserById(userId) ;
        yunUserVO.setYunUsers(yunUsers);
        String hqlDisease = "select di from YunUserDisease as du,YunDiseaseList di where di.dcode=du.dcode and  du.userId='"+userId+"'";
        List<YunDiseaseList> yunUserDisease = userFacade.createQuery(YunDiseaseList.class,hqlDisease,new ArrayList<Object>()).getResultList();
        yunUserVO.setYunUserDisease(yunUserDisease);
        String diseaseManagerHql = "select di from YunUserDiseaseManager dm,YunDiseaseList di where di.dcode=dm.dcode and dm.userId='"+userId+"'";
        List<YunDiseaseList> yunUserDiseaseManagers = userFacade.createQuery(YunDiseaseList.class,diseaseManagerHql,new ArrayList<Object>()).getResultList();
        yunUserVO.setYunUserDiseaseManager(yunUserDiseaseManagers);
        return yunUserVO;
    }


    /**
     * 修改研究疾病和管理疾病
     * @param yunUserVO
     * @return
     */
    @POST
    @Transactional
    @Path("merge-user-disease-info")
    public Response mergeYunUserDiseaseInfo(YunUserVO yunUserVO){
        YunUsers yunUsers = yunUserVO.getYunUsers() ;
        List<YunDiseaseList> yunUserDiseasese = yunUserVO.getYunUserDisease();
        List<YunDiseaseList> yunUserDiseaseManager = yunUserVO.getYunUserDiseaseManager();
        String hql = "delete from YunUserDisease as yd where yd.userId='"+yunUsers.getId()+"'" ;
        String hql2 = "delete from YunUserDiseaseManager as ym where ym.userId='"+yunUsers.getId()+"'" ;

        userFacade.removeByHql(hql);
        userFacade.removeByHql(hql2);

        for(YunDiseaseList diseaseList:yunUserDiseasese){
            YunUserDisease yunUserDisease = new YunUserDisease() ;
            yunUserDisease.setUserId(yunUsers.getId());
            yunUserDisease.setDcode(diseaseList.getDcode());
            userFacade.merge(yunUserDisease) ;
        }

        for (YunDiseaseList yunDiseaseList:yunUserDiseaseManager){
            YunUserDiseaseManager yunUserDiseaseManager1 = new YunUserDiseaseManager() ;
            yunUserDiseaseManager1.setUserId(yunUsers.getId());
            yunUserDiseaseManager1.setDcode(yunDiseaseList.getDcode());
            userFacade.merge(yunUserDiseaseManager1) ;
        }
        return Response.status(Response.Status.OK).entity(yunUsers).build();
    }


    /**
     * 获取用户列表，肯根据用户状态，用户状态不传递或者传递为空则获取全部用户
     * @param where
     * @return
     */
    @GET
    @Path("user-list")
    public Page<YunUsers> listYunUsersByFlags(@QueryParam("loginFlag") String loginFlag,@QueryParam("userName") String userName,
                                              @QueryParam("userId")String userId,@QueryParam("email")String email,
                                              @QueryParam("rolename")String rolename,@QueryParam("where")String where,@QueryParam("mobile")String mobile,
                                              @QueryParam("perPage")int perPage,@QueryParam("currentPage") int currentPage){
        String hql = "from YunUsers as user where 1=1 " ;
        String hqlCount = "select count(user) from YunUsers as user where 1=1 " ;
        if(!"".equals(where)&&where!=null){
            hql+=(" and "+where);
            hqlCount+=(" and "+where);
        }
        if(!"".equals(loginFlag)&&loginFlag!=null){
            hql+=" and user.loginFlags='"+loginFlag+"'";
            hqlCount+=" and user.loginFlags='"+loginFlag+"'";
        }
        if(!"".equals(mobile)&&mobile!=null){
            hql+=" and user.mobile='"+mobile+"'";
            hqlCount+=" and user.mobile='"+mobile+"'";
        }
        if(!"".equals(userName)&&userName!=null){
            hql+=" and user.userName='"+userName+"'";
            hqlCount+=" and user.userName='"+userName+"'";
        }
        if(!"".equals(userId)&&userId!=null){
            hql+=" and user.userId='"+userId+"'";
            hqlCount+=" and user.userId='"+userId+"'";
        }
        if(!"".equals(email)&&email!=null){
            hql+=" and user.email='"+email+"'";
            hqlCount+=" and user.email='"+email+"'";
        }
        if(!"".equals(rolename)&&rolename!=null){
            hql+=" and user.rolename='"+rolename+"'";
            hqlCount+=" and user.rolename='"+rolename+"'";
        }

        hql+=" order by user.createDate desc";
        hqlCount+=" order by user.createDate desc";
        TypedQuery<YunUsers> query = userFacade.createQuery(YunUsers.class, hql, new ArrayList<Object>());
        Page<YunUsers> yunUsersPage = new Page<>();
        Long counts = userFacade.createQuery(Long.class, hqlCount, new ArrayList<Object>()).getSingleResult();
        yunUsersPage.setCounts(counts);
        if(currentPage<=0){
            currentPage = 1;
        }
        if(perPage>0){
            query.setFirstResult((currentPage-1)*perPage) ;
            query.setMaxResults(perPage);
            yunUsersPage.setPerPage((long) perPage);

        }
        List<YunUsers> resultList = query.getResultList();
        yunUsersPage.setData(resultList);

        return yunUsersPage;
    }


    /**
     * 添加用户角色
     * @param roleDicts
     * @param userId
     * @return
     */
    @Transactional
    @POST
    @Path("add-user-role")
    public Response addRoles(List<RoleDict> roleDicts,@QueryParam("userId") String userId) throws Exception {
        YunUsers yunUserById = userFacade.getYunUserById(userId);
//        String rvHql = "select d from RoleVsUser as r,RoleDict as d where r.roleId = d.id and r.userId='"+userId+"'";
//        List<RoleDict> roleDictList = userFacade.createQuery(RoleDict.class,rvHql,new ArrayList<Object>()).getResultList();
//        Boolean isAssistan = false;
//        if(roleDictList!=null && !roleDictList.isEmpty()){
//            for(RoleDict roleDict:roleDictList){
//                if(SmsSendUtil.getStringByKey("roleCode").equals(roleDict.getCode())){//研究助手
//                    isAssistan = true;
//                    if(!roleDicts.get(0).getCode().equals(roleDict.getCode())){
//                        throw new Exception("不允许变更研究助手权限");
//                    }
//                }
//            }
//        }
//        for(RoleDict dict:roleDicts){
//            if(!isAssistan && SmsSendUtil.getStringByKey("roleCode").equals(dict.getCode())){
//                throw new Exception("不允许将权限变更为研究助手权限");
//            }
//        }
        List<RoleVsUser> roleVsUsers = new ArrayList<>() ;
        String hql = "delete from RoleVsUser as r where r.userId='"+userId+"'" ;
        userFacade.excHql(hql);
        for(RoleDict roleDict:roleDicts){
            RoleVsUser roleVsUser = new RoleVsUser();
            roleVsUser.setUserId(userId);
            roleVsUser.setRoleId(roleDict.getId());
            roleVsUsers.add(userFacade.merge(roleVsUser));
        }
        return Response.status(Response.Status.OK).entity(roleVsUsers).build();
    }

    /**
     * 获取用户
     * @param id
     * @return
     * @throws Exception
     */
    @GET
    @Path("get-user-by-id")
    public YunUsers getYunUser(@QueryParam("userId") String id) throws Exception {
        return userFacade.getYunUserById(id);
    }

    /**
     * 手机短信验证码 获取
     * @param loginName 登录用户名
     * @return
     * @throws Exception
     */
    @GET
    @Path("get-very-code")
    public List getVeryCode(@QueryParam("loginName") String loginName,@Context HttpServletRequest request) throws Exception{
        List<String> list = new ArrayList<>();
        YunUsers yunUsers = userFacade.getYunUsersByLoginName(loginName);
        if(yunUsers.getMobile()==null || "".equals(yunUsers.getMobile())){
            throw new Exception("用户未绑定手机号，请修改个人信息进行手机号码绑定");
        }
        if(!SmsSendUtil.isMobile(yunUsers.getMobile())){
            throw new Exception("用户手机号不正确，请修改手机号");
        }
        String veryCode = SmsSendUtil.getInstance().execSendCode(yunUsers.getMobile(),"reset");
        list.add(loginName);
        if(request!=null){
            request.getSession().setAttribute(request.getSession().getId(),veryCode);
        }
        return list;
    }

    /**
     * 忘记密码 设置新密码
     * @param userName
     * @param veryCode
     * @param newPassword
     * @param confirmPassword
     * @param request
     * @return
     * @throws Exception
     */
    @POST
    @Path("reset-user-pwd")
    @Transactional
    public Response resetUserPassWord(@QueryParam("loginName") String userName,@QueryParam("veryCode") String veryCode,
                                       @QueryParam("newPassword") String newPassword, @QueryParam("confirmPassword") String confirmPassword,
                                       @Context HttpServletRequest request) throws Exception {
        YunUsers yunUsers = userFacade.getYunUsersByLoginName(userName);
        if(StringUtils.isEmpty(veryCode)){
            throw new Exception("请输入验证码");
        }
        String sessionVeryCode = (String)request.getSession().getAttribute(request.getSession().getId());
        if(StringUtils.isEmpty(sessionVeryCode)){
            throw new Exception("验证码已失效，请重新获取");
        }
        if(!veryCode.equals(sessionVeryCode)){
            throw new Exception("验证码不正确，请重新输入");
        }
        if(StringUtils.isEmpty(newPassword)){
            throw new Exception("输入密码不能为空");
        }
        if(StringUtils.isEmpty(confirmPassword)){
            throw new Exception("确认密码不能为空");
        }
        if(!newPassword.equals(confirmPassword)){
            throw new Exception("输入密码和确认密码不一致，请重新输入");
        }
        PasswordAndSalt passwordAndSalt = SystemPasswordService.enscriptPassword(yunUsers.getUserId(), confirmPassword);
        yunUsers.setPassword(passwordAndSalt.getPassword());
        yunUsers.setSalt(passwordAndSalt.getSalt());
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        request.getSession().removeAttribute(request.getSession().getId());
        return Response.status(Response.Status.OK).entity(userFacade.merge(yunUsers)).build();
    }

    /**
     *通过手机号获取验证码
     * @param mobile 手机号
     * @param request
     * @return
     * @throws Exception
     */
    @GET
    @Path("get-very-code-by-mobile")
    public List<String> getVeryCodeByPhone(@QueryParam("pictureCode") String pictureCode,@QueryParam("mobile") String mobile,@Context HttpServletRequest request) throws Exception{
        List<String> list = new ArrayList<>();
        if(StringUtils.isEmpty(pictureCode)){
            throw new Exception("图形验证码不能为空");
        }
        String sessionPictureCode = request.getSession().getAttribute(request.getSession().getId()+SmsSendUtil.pictureCode)==null?"":request.getSession().getAttribute(request.getSession().getId()+SmsSendUtil.pictureCode)+"";
        if(!pictureCode.equals(sessionPictureCode)){
            throw new Exception("图形验证码输入错误，请重新输入");
        }
        if(StringUtils.isEmpty(mobile)){
            throw new Exception("手机号不能为空");
        }
        if(!SmsSendUtil.isMobile(mobile)){
            throw new Exception("用户手机号不正确，请重新输入");
        }
//        if(mobileMap.get(mobile)!=null){
//            Integer number = mobileMap.get(mobile)+1;
//            Long time = timeMap.get(mobile);
//            Long now = new Date().getTime();
//            if((now-time)<60000 && number>1){
//                throw new Exception("请求过于频繁，请稍后重试");
//            }else if((now-time)<60*60*1000 && number>3){
//                throw new Exception("请求过于频繁，请稍后重试");
//            }else if((now-time)>=60*60*1000){
//                mobileMap.put(mobile,1);
//                timeMap.put(mobile,new Date().getTime());
//            }else{
//                mobileMap.put(mobile,number);
//            }
//        }else{
//            mobileMap.put(mobile,1);
//            timeMap.put(mobile,new Date().getTime());
//        }

        String veryCode = SmsSendUtil.getInstance().execSendCode(mobile,SmsSendUtil.register);
        list.add(mobile);
        if(request!=null){
            request.getSession().setAttribute(request.getSession().getId()+SmsSendUtil.register,veryCode);
            request.getSession().removeAttribute(request.getSession().getId()+SmsSendUtil.pictureCode);//清除图形验证码
            request.getSession().setAttribute(request.getSession().getId()+SmsSendUtil.pictureCodeToRegister,sessionPictureCode);
            request.getSession().setAttribute(request.getSession().getId()+_Mobile,mobile);
        }
        return list;
    }

    /**
     * 获取注册图形验证码
     * @param request
     * @return
     */
    @GET
    @Path("get-picture-code")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getPictureCode(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
        StringBuffer pictureCode = new StringBuffer("");
        int width = 120;
        int height = 41;
        int lines = 10;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        // 设置字体
        g.setFont(new Font("宋体", Font.BOLD, 20));
        // 随机数字
        Random r = new Random(new Date().getTime());
        for (int i = 0; i < 4; i++) {
            int a = r.nextInt(10);
            int y = 10 + r.nextInt(20);// 10~30范围内的一个整数，作为y坐标

            Color c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
            g.setColor(c);

            g.drawString("" + a, 5 + i * width / 4, y);
            pictureCode.append(a);
        }
        // 干扰线
        for (int i = 0; i < lines; i++) {
            Color c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
            g.setColor(c);
            g.drawLine(r.nextInt(width), r.nextInt(height), r.nextInt(width), r.nextInt(height));
        }
        g.dispose();// 类似于流中的close()带动flush()---把数据刷到img对象当中
        ImageIO.write(img, "JPG", response.getOutputStream());
        request.getSession().setAttribute(request.getSession().getId()+SmsSendUtil.pictureCode,pictureCode.toString());
        request.getSession().setAttribute(request.getSession().getId()+SmsSendUtil.pictureCodeToRegister,pictureCode.toString());
        return Response.status(Response.Status.OK).entity(response.getOutputStream()).header("Content-disposition","attachment;filename="+"图形码")
                .header("Cache-Control","no-cache").build();
    }
    /**
     *  获取删除病人手机短信验证码
     * @param loginName 登录用户名
     * @return
     * @throws Exception
     */
    @GET
    @Path("get-del-pat-very-code")
    public List getDelPatientVeryCode(@QueryParam("loginName") String loginName,@Context HttpServletRequest request) throws Exception{
        List<String> list = new ArrayList<>();
        YunUsers yunUsers = userFacade.getYunUsersByLoginName(loginName);
        if(yunUsers.getMobile()==null || "".equals(yunUsers.getMobile())){
            throw new Exception("用户未绑定手机号，请修改个人信息进行手机号码绑定");
        }
        if(!SmsSendUtil.isMobile(yunUsers.getMobile())){
            throw new Exception("用户手机号不正确，请修改手机号");
        }
        String veryCode = SmsSendUtil.getInstance().execSendCode(yunUsers.getMobile(),"delPatient");
        list.add(loginName);
        if(request!=null){
            request.getSession().setAttribute(request.getSession().getId()+SmsSendUtil.delPationt,veryCode);
        }
        return list;
    }

    /**
     * 更新用户
     * @param yunUsers
     * @return
     */
    @POST
    @Transactional
    @Path("audit-user")
    public Response auditYunUser(YunUsers yunUsers) throws Exception {
        String id = yunUsers.getId();
        if(id==null||"".equals(id)){
            System.out.println(id);
            throw new Exception("获取不到原信息的ID");
        }
        YunUsers dbUsers = userFacade.get(YunUsers.class,id);
        String loginFlags = dbUsers.getLoginFlags();
        dbUsers.setLoginFlags(yunUsers.getLoginFlags());
        dbUsers.setRolename(yunUsers.getRolename());
        YunUsers users = userFacade.merge(dbUsers);
        if(!users.getLoginFlags().equals(loginFlags) && "R".equals(users.getLoginFlags())){//表示审核通过
            String mailInfo = "您好:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;您的账号"+yunUsers.getUserName()+"已通过审核，您可以进行登录了，登录连接<a href=\"http://nrdrs.org\">http://nrdrs.org</a>";
            mailSendFacade.sendMail("用户审核",users.getEmail(),mailInfo);
            mailSendFacade.sendMessageToUser(yunUsers.getId(),"用户审核",mailInfo);
        }
        String hql = "delete from RoleVsUser as r where r.userId='"+yunUsers.getId()+"'" ;
        userFacade.excHql(hql);
        String roleHql = " from RoleDict where code = '"+yunUsers.getRolename()+"'";
        List<RoleDict> roleDicts = userFacade.createQuery(RoleDict.class,roleHql,new ArrayList<Object>()).getResultList();
        for(RoleDict roleDict:roleDicts){
            RoleVsUser roleVsUser = new RoleVsUser();
            roleVsUser.setUserId(yunUsers.getId());
            roleVsUser.setRoleId(roleDict.getId());
            userFacade.merge(roleVsUser);
        }
        return Response.status(Response.Status.OK).entity(users).build();
    }
}
