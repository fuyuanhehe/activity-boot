package boot.spring.controller;

import java.io.InputStream;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import boot.spring.pagemodel.Process;
import boot.spring.pagemodel.DataGrid;
import boot.spring.pagemodel.HistoryProcess;
import boot.spring.pagemodel.LeaveTask;
import boot.spring.pagemodel.MSG;
import boot.spring.pagemodel.RunningProcess;
import boot.spring.po.LeaveApply;
import boot.spring.po.Permission;
import boot.spring.po.Role;
import boot.spring.po.Role_permission;
import boot.spring.po.User;
import boot.spring.po.User_role;
import boot.spring.service.LeaveService;
import boot.spring.service.SystemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;




@Api(value = "请假流程接口")
@Controller
public class ActivitiController
{
    @Autowired
    RepositoryService rep;
    @Autowired
    RuntimeService runservice;
    @Autowired
    FormService formservice;
    @Autowired
    IdentityService identityservice;
    @Autowired
    LeaveService leaveservice;
    @Autowired
    TaskService taskservice;
    @Autowired
    HistoryService histiryservice;
    @Autowired
    SystemService systemservice;

    @RequestMapping(value = "/processlist", method = RequestMethod.GET)
    String process()
    {
        return "activiti/processlist";
    }

    // 上传bpm图 上传工作流文件
    @RequestMapping(value = "/uploadworkflow", method = RequestMethod.POST)
    public String fileupload(@RequestParam MultipartFile uploadfile )
    {
        try
        {
            MultipartFile file = uploadfile;
            String filename = file.getOriginalFilename();
            InputStream is = file.getInputStream();
            rep.createDeployment().addInputStream(filename, is).deploy();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return "index";
    }

    /**
     * 功能描述: 已部署的流程
     * @param:
     * @return:
     * @auther: fuyuan
     * @date: 2019/12/31 0031 10:21
     */
    @RequestMapping(value = "/getprocesslists", method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<Process> getlist(@RequestParam("current") int current, @RequestParam("rowCount") int rowCount)
    {
        int firstrow = (current - 1) * rowCount;
        List<ProcessDefinition> list = rep.createProcessDefinitionQuery().listPage(firstrow, rowCount);
        int total = rep.createProcessDefinitionQuery().list().size();
        List<Process> mylist = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            Process p = new Process();
            p.setDeploymentId(list.get(i).getDeploymentId());
            p.setId(list.get(i).getId());
            p.setKey(list.get(i).getKey());
            p.setName(list.get(i).getName());
            p.setResourceName(list.get(i).getResourceName());
            p.setDiagramresourcename(list.get(i).getDiagramResourceName());
            mylist.add(p);
        }
        DataGrid<Process> grid = new DataGrid<>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        grid.setRows(mylist);
        grid.setTotal(total);
        return grid;
    }

    @RequestMapping(value = "/showresource", method = RequestMethod.GET)
    public void export(@RequestParam("pdid") String pdid, @RequestParam("resource") String resource,
            HttpServletResponse response) throws Exception
    {
        ProcessDefinition def = rep.createProcessDefinitionQuery().processDefinitionId(pdid).singleResult();
        InputStream is = rep.getResourceAsStream(def.getDeploymentId(), resource);
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(is, output);
    }

    @RequestMapping(value = "/deletedeploy", method = RequestMethod.POST)
    public String deletedeploy(@RequestParam("deployid") String deployid)
    {
        rep.deleteDeployment(deployid, true);
        return "activiti/processlist";
    }

    @RequestMapping(value = "/runningprocess", method = RequestMethod.GET)
    public String task()
    {
        return "activiti/runningprocess";
    }

    @RequestMapping(value = "/deptleaderaudit", method = RequestMethod.GET)
    public String mytask()
    {
        return "activiti/deptleaderaudit";
    }

    @RequestMapping(value = "/hraudit", method = RequestMethod.GET)
    public String hr()
    {
        return "activiti/hraudit";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String my()
    {
        return "index";
    }

    @RequestMapping(value = "/leaveapply", method = RequestMethod.GET)
    public String leave()
    {
        return "activiti/leaveapply";
    }

    @RequestMapping(value = "/reportback", method = RequestMethod.GET)
    public String reprotback()
    {
        return "activiti/reportback";
    }

    @RequestMapping(value = "/modifyapply", method = RequestMethod.GET)
    public String modifyapply()
    {
        return "activiti/modifyapply";
    }

    /**
     * 功能描述: 启动流程
     * @auther: fuyuan
     * @date: 2019/12/30 0030 14:32
     */
    @RequestMapping(value = "/startleave", method = RequestMethod.POST)
    @ResponseBody
    public MSG start_leave(LeaveApply apply, HttpSession session)
    {
        // 获取用户名称
        String userid = (String) session.getAttribute("username");

        // 制定组任务-人
        Map<String, Object> variables = new HashMap<>();
        variables.put("applyuserid", userid);

        ProcessInstance ins = leaveservice.startWorkflow(apply, userid, variables);
        System.out.println("流程id" + ins.getId() + "已启动");
        return new MSG("success");
    }
     /**
      * ACT_RU_TASK 连接 ACT_RU_IDENTITYLINK 运行时流程人员表
      * 根据  ACT_RU_IDENTITYLINK：GROUP_ID_ 部门经理 查询任务表数据
      *  接受任务人已指定为  部门经理
      * 功能描述:
      * @auther: fuyuan
      * @date: 2020/4/14 0014 15:06
      */
    @ApiOperation("获取部门领导审批代办列表")
    @RequestMapping(value = "/depttasklist", produces =
    { "application/json;charset=UTF-8" }, method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<LeaveTask> getdepttasklist(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        DataGrid<LeaveTask> grid = new DataGrid<>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(0);
        grid.setRows(new ArrayList<>());
        // 先做权限检查，对于没有部门领导审批权限的用户,直接返回空
        String userid = (String) session.getAttribute("username");
        int uid = systemservice.getUidByusername(userid);
        User user = systemservice.getUserByid(uid);
        List<User_role> userroles = user.getUser_roles();
        if (userroles == null)
            return grid;
        boolean flag = false;// 默认没有权限
        for (int k = 0; k < userroles.size(); k++)
        {
            User_role ur = userroles.get(k);
            Role r = ur.getRole();
            int roleid = r.getRid();
            Role role = systemservice.getRolebyid(roleid);
            List<Role_permission> p = role.getRole_permission();
            for (int j = 0; j < p.size(); j++)
            {
                Role_permission rp = p.get(j);
                Permission permission = rp.getPermission();
                // 查询用户菜单是否有部门领导审批 权限
                if (permission.getPermissionname().equals("部门领导审批"))
                    flag = true;
                else
                    continue;
            }
        }
        if (flag == false)// 无权限
        {
            return grid;
        } else
        {
            int firstrow = (current - 1) * rowCount;
            //查询数据
            List<LeaveApply> results = leaveservice.getpagedepttask(userid, firstrow, rowCount);
            //查询条数
            int totalsize = leaveservice.getalldepttask(userid);


            List<LeaveTask> tasks = new ArrayList<>();
            for (LeaveApply apply : results)
            {
                LeaveTask task = new LeaveTask();
                task.setApply_time(apply.getApply_time());
                task.setUser_id(apply.getUser_id());
                task.setEnd_time(apply.getEnd_time());
                task.setId(apply.getId());
                task.setLeave_type(apply.getLeave_type());
                task.setProcess_instance_id(apply.getProcess_instance_id());
                task.setProcessdefid(apply.getTask().getProcessDefinitionId());
                task.setReason(apply.getReason());
                task.setStart_time(apply.getStart_time());
                task.setTaskcreatetime(apply.getTask().getCreateTime());
                task.setTaskid(apply.getTask().getId());
                task.setTaskname(apply.getTask().getName());
                tasks.add(task);
            }
            grid.setRowCount(rowCount);
            grid.setCurrent(current);
            grid.setTotal(totalsize);
            grid.setRows(tasks);
            return grid;
        }
    }

    /**
     * 功能描述: 查询人事任务
     * @auther: fuyuan
     * @date: 2020/4/13 0013 14:33
     */
    @RequestMapping(value = "/hrtasklist", produces =
    { "application/json;charset=UTF-8" }, method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<LeaveTask> gethrtasklist(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        DataGrid<LeaveTask> grid = new DataGrid<>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(0);
        grid.setRows(new ArrayList<>());
        // 先做权限检查，对于没有人事权限的用户,直接返回空
        String userid = (String) session.getAttribute("username");
        int uid = systemservice.getUidByusername(userid);
        User user = systemservice.getUserByid(uid);
        List<User_role> userroles = user.getUser_roles();
        if (userroles == null)
            return grid;
        boolean flag = false;// 默认没有权限
        for (int k = 0; k < userroles.size(); k++)
        {
            User_role ur = userroles.get(k);
            Role r = ur.getRole();
            int roleid = r.getRid();
            Role role = systemservice.getRolebyid(roleid);
            List<Role_permission> p = role.getRole_permission();
            for (int j = 0; j < p.size(); j++)
            {
                Role_permission rp = p.get(j);
                Permission permission = rp.getPermission();
                if (permission.getPermissionname().equals("人事审批"))
                    flag = true;
                else
                    continue;
            }
        }
        if (flag == false)// 无权限
        {
            return grid;
        } else
        {
            int firstrow = (current - 1) * rowCount;
            List<LeaveApply> results = leaveservice.getpagehrtask(userid, firstrow, rowCount);
            int totalsize = leaveservice.getallhrtask(userid);
            List<LeaveTask> tasks = new ArrayList<>();
            for (LeaveApply apply : results)
            {
                LeaveTask task = new LeaveTask();
                task.setApply_time(apply.getApply_time());
                task.setUser_id(apply.getUser_id());
                task.setEnd_time(apply.getEnd_time());
                task.setId(apply.getId());
                task.setLeave_type(apply.getLeave_type());
                task.setProcess_instance_id(apply.getProcess_instance_id());
                task.setProcessdefid(apply.getTask().getProcessDefinitionId());
                task.setReason(apply.getReason());
                task.setStart_time(apply.getStart_time());
                task.setTaskcreatetime(apply.getTask().getCreateTime());
                task.setTaskid(apply.getTask().getId());
                task.setTaskname(apply.getTask().getName());
                tasks.add(task);
            }
            grid.setRowCount(rowCount);
            grid.setCurrent(current);
            grid.setTotal(totalsize);
            grid.setRows(tasks);
            return grid;
        }
    }

    /**
     * 功能描述: 销假
     * @auther: fuyuan
     * @date: 2020/4/13 0013 14:40
     */
    @RequestMapping(value = "/xjtasklist", produces =
    { "application/json;charset=UTF-8" }, method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<LeaveTask> getXJtasklist(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        int firstrow = (current - 1) * rowCount;
        String userid = (String) session.getAttribute("username");
        List<LeaveApply> results = leaveservice.getpageXJtask(userid, firstrow, rowCount);
        int totalsize = leaveservice.getallXJtask(userid);
        List<LeaveTask> tasks = new ArrayList<>();
        for (LeaveApply apply : results)
        {
            LeaveTask task = new LeaveTask();
            task.setApply_time(apply.getApply_time());
            task.setUser_id(apply.getUser_id());
            task.setEnd_time(apply.getEnd_time());
            task.setId(apply.getId());
            task.setLeave_type(apply.getLeave_type());
            task.setProcess_instance_id(apply.getProcess_instance_id());
            task.setProcessdefid(apply.getTask().getProcessDefinitionId());
            task.setReason(apply.getReason());
            task.setStart_time(apply.getStart_time());
            task.setTaskcreatetime(apply.getTask().getCreateTime());
            task.setTaskid(apply.getTask().getId());
            task.setTaskname(apply.getTask().getName());
            tasks.add(task);
        }
        DataGrid<LeaveTask> grid = new DataGrid<>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(totalsize);
        grid.setRows(tasks);
        return grid;
    }
    /**
     * 功能描述: 调整
     * @auther: fuyuan
     * @date: 2020/4/13 0013 14:40
     */
    @RequestMapping(value = "/updatetasklist", produces =
    { "application/json;charset=UTF-8" }, method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<LeaveTask> getupdatetasklist(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        int firstrow = (current - 1) * rowCount;
        String userid = (String) session.getAttribute("username");
        List<LeaveApply> results = leaveservice.getpageupdateapplytask(userid, firstrow, rowCount);
        int totalsize = leaveservice.getallupdateapplytask(userid);
        List<LeaveTask> tasks = new ArrayList<>();
        for (LeaveApply apply : results)
        {
            LeaveTask task = new LeaveTask();
            task.setApply_time(apply.getApply_time());
            task.setUser_id(apply.getUser_id());
            task.setEnd_time(apply.getEnd_time());
            task.setId(apply.getId());
            task.setLeave_type(apply.getLeave_type());
            task.setProcess_instance_id(apply.getProcess_instance_id());
            task.setProcessdefid(apply.getTask().getProcessDefinitionId());
            task.setReason(apply.getReason());
            task.setStart_time(apply.getStart_time());
            task.setTaskcreatetime(apply.getTask().getCreateTime());
            task.setTaskid(apply.getTask().getId());
            task.setTaskname(apply.getTask().getName());
            tasks.add(task);
        }
        DataGrid<LeaveTask> grid = new DataGrid<>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(totalsize);
        grid.setRows(tasks);
        return grid;
    }

    /**
     * 传入 taskid 查ACT_RU_TASK 获取实例id 查act_ru_execution实例表获取 buskey 在查业务表 ->
     * 功能描述: 查询请假单
     * @auther: fuyuan
     * @date: 2019/12/30 0030 14:44
     */
    @RequestMapping(value = "/dealtask", method = RequestMethod.POST)
    @ResponseBody
    public LeaveApply taskdeal(@RequestParam("taskid") String taskid)
    {
        Task task = taskservice.createTaskQuery().taskId(taskid).singleResult();
        ProcessInstance process = runservice.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId())
                .singleResult();
        LeaveApply leave = leaveservice.getleave( Integer.parseInt(process.getBusinessKey()));

 /*       Map<String,Object> map = new HashMap<>();
        String approve = req.getParameter("deptleaderapprove");


        map.put("deptleaderapprove",approve); // 部门领导
     //   map.put("hrapprove",true); // 人事
    //    map.put("reapply","true"); //调整
        // deptleaderapprove
        taskservice.complete(taskid,map);// 处理任务*/

        return leave;
    }

    @RequestMapping(value = "/activiti/task-deptleaderaudit", method = RequestMethod.GET)
    String url()
    {
        return "/activiti/task-deptleaderaudit";
    }

    /**传入 taskid 查ACT_RU_TASK 获取实例id 查act_ru_execution实例表获取 buskey 在查业务表 ->
     *  完成任务
     * 功能描述: 处理任务 部门领导
     * @auther: fuyuan
     * @date: 2019/12/30 0030 14:58
     */
    @RequestMapping(value = "/task/deptcomplete/{taskid}", method = RequestMethod.POST)
    @ResponseBody
    public MSG deptcomplete(HttpSession session, @PathVariable("taskid") String taskid, HttpServletRequest req)
    {
        String userid = (String) session.getAttribute("username");

        Map<String, Object> variables = new HashMap<>();
        String approve = req.getParameter("deptleaderapprove");

        // true or false
        variables.put("deptleaderapprove", approve);
        // 指定人 领取任务
        taskservice.claim(taskid, userid);
        // 完成任务
        taskservice.complete(taskid, variables);

        return new MSG("success");
    }

    /**
     * 功能描述: 人事审批
     * @auther: fuyuan
     * @date: 2019/12/30 0030 15:03
     */
    @RequestMapping(value = "/task/hrcomplete/{taskid}", method = RequestMethod.POST)
    @ResponseBody
    public MSG hrcomplete(HttpSession session, @PathVariable("taskid") String taskid, HttpServletRequest req)
    {
        String userid = (String) session.getAttribute("username");


        Map<String, Object> variables = new HashMap<>();
        String approve = req.getParameter("hrapprove");// true

        variables.put("hrapprove", approve);


        taskservice.claim(taskid, userid);
        taskservice.complete(taskid, variables);
        return new MSG("success");
    }

    @RequestMapping(value = "/task/reportcomplete/{taskid}", method = RequestMethod.POST)
    @ResponseBody
    public MSG reportbackcomplete(@PathVariable("taskid") String taskid, HttpServletRequest req)
    {
        String realstart_time = req.getParameter("realstart_time");
        String realend_time = req.getParameter("realend_time");
        leaveservice.completereportback(taskid, realstart_time, realend_time);
        return new MSG("success");
    }

    /**
     * 功能描述: 重新调整
     * @auther: fuyuan
     * @date: 2019/12/30 0030 15:04
     */
    @RequestMapping(value = "/task/updatecomplete/{taskid}", method = RequestMethod.POST)
    @ResponseBody
    public MSG updatecomplete(@PathVariable("taskid") String taskid, @ModelAttribute("leave") LeaveApply leave,
            @RequestParam("reapply") String reapply)
    {
        // reapply true or false
        leaveservice.updatecomplete(taskid, leave, reapply);
        return new MSG("success");
    }

    /**
     * 功能描述: // 参与的正在运行的请假流程
     * @auther: fuyuan
     * @date: 2019/12/31 0031 10:35
     */
    @RequestMapping(value = "involvedprocess", method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<RunningProcess> allexeution(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        int firstrow = (current - 1) * rowCount;
        String userid = (String) session.getAttribute("username");
        ProcessInstanceQuery query = runservice.createProcessInstanceQuery();
        int total = (int) query.count();

        // 结果: res.*
        // 	ACT_RU_EXECUTION RES
        //	INNER JOIN ACT_RE_PROCDEF P ON RES.PROC_DEF_ID_ = P.ID_
        // P.KEY_ = 'leave'   I.USER_ID_ = 'admin'

        List<ProcessInstance> a = query.processDefinitionKey("leave").involvedUser(userid).listPage(firstrow, rowCount);
        List<RunningProcess> list = new ArrayList<>();
        RunningProcess process = null;
        for (ProcessInstance p : a)
        {
             process = new RunningProcess();
            process.setActivityid(p.getActivityId());
            process.setBusinesskey(p.getBusinessKey());
            process.setExecutionid(p.getId());
            process.setProcessInstanceid(p.getProcessInstanceId());
            list.add(process);
        }
        DataGrid<RunningProcess> grid = new DataGrid<>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        grid.setTotal(total);
        grid.setRows(list);
        return grid;
    }

    /**
     * 功能描述: 流程历史记录查询-已结束的
     * @auther: fuyuan
     * @date: 2019/12/31 0031 10:39
     */
    @RequestMapping(value = "/getfinishprocess", method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<HistoryProcess> getHistory(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        String userid = (String) session.getAttribute("username");
        /** res.*
         * 	ACT_HI_PROCINST RES
         * 	LEFT OUTER JOIN ACT_RE_PROCDEF DEF ON RES.PROC_DEF_ID_ = DEF.ID_
         *
         * 	DEF.KEY_ = leave   RES.START_USER_ID_ = admin
         */
        HistoricProcessInstanceQuery process = histiryservice.createHistoricProcessInstanceQuery()
                .processDefinitionKey("leave").startedBy(userid).finished();
        int total = (int) process.count();
        int firstrow = (current - 1) * rowCount;
        /**
         *  res.*
         * ACT_HI_PROCINST RES 历史流程实例表
         * 	LEFT OUTER JOIN ACT_RE_PROCDEF DEF ON RES.PROC_DEF_ID_ = DEF.ID_
         *
         * 	 	DEF.KEY_ = 'leave'  RES.START_USER_ID_ = 'admin'
         */
        List<HistoricProcessInstance> info = process.listPage(firstrow, rowCount);
        List<HistoryProcess> list = new ArrayList<>();
        HistoryProcess his = null;
        String bussinesskey = null;
        for (HistoricProcessInstance history : info)
        {
             his = new HistoryProcess();
             bussinesskey = history.getBusinessKey();
            LeaveApply apply = leaveservice.getleave(Integer.parseInt(bussinesskey));
            his.setLeaveapply(apply);
            his.setBusinessKey(bussinesskey);
            his.setProcessDefinitionId(history.getProcessDefinitionId());
            list.add(his);
        }
        DataGrid<HistoryProcess> grid = new DataGrid<>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        grid.setTotal(total);
        grid.setRows(list);
        return grid;
    }

    @RequestMapping(value = "/historyprocess", method = RequestMethod.GET)
    public String history()
    {
        return "activiti/historyprocess";
    }

    @RequestMapping(value = "/processinfo", method = RequestMethod.POST)
    @ResponseBody
    public List<HistoricActivityInstance> processinfo(@RequestParam("instanceid") String instanceid)
    {
        List<HistoricActivityInstance> his = histiryservice.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceid).orderByHistoricActivityInstanceStartTime().asc().list();
        return his;
    }

    @RequestMapping(value = "/processhis", method = RequestMethod.POST)
    @ResponseBody
    public List<HistoricActivityInstance> processhis(@RequestParam("ywh") String ywh)
    {
        String instanceid = histiryservice.createHistoricProcessInstanceQuery().processDefinitionKey("purchase")
                .processInstanceBusinessKey(ywh).singleResult().getId();
        List<HistoricActivityInstance> his = histiryservice.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceid).orderByHistoricActivityInstanceStartTime().asc().list();
        return his;
    }

    @RequestMapping(value = "myleaveprocess", method = RequestMethod.GET)
    String myleaveprocess()
    {
        return "activiti/myleaveprocess";
    }

    /**
     * 功能描述: 流程图查看（现流程到哪里了）
     * @auther: fuyuan
     * @date: 2019/12/31 0031 10:27
     */
    @RequestMapping(value = "traceprocess/{executionid}", method = RequestMethod.GET)
    public void traceprocess(@PathVariable("executionid") String executionid, HttpServletResponse response)
            throws Exception
    {
        ProcessInstance process = runservice.createProcessInstanceQuery().processInstanceId(executionid).singleResult();
        BpmnModel bpmnmodel = rep.getBpmnModel(process.getProcessDefinitionId());
        List<String> activeActivityIds = runservice.getActiveActivityIds(executionid);
        DefaultProcessDiagramGenerator gen = new DefaultProcessDiagramGenerator();
        // 获得历史活动记录实体（通过启动时间正序排序，不然有的线可以绘制不出来）
        List<HistoricActivityInstance> historicActivityInstances = histiryservice.createHistoricActivityInstanceQuery()
                .executionId(executionid).orderByHistoricActivityInstanceStartTime().asc().list();
        // 计算活动线
        List<String> highLightedFlows = leaveservice
                .getHighLightedFlows(
                        (ProcessDefinitionEntity) ((RepositoryServiceImpl) rep)
                                .getDeployedProcessDefinition(process.getProcessDefinitionId()),
                        historicActivityInstances);

        InputStream in = gen.generateDiagram(bpmnmodel, "png", activeActivityIds, highLightedFlows, "宋体", "宋体", null,
                null, 1.0);
        // InputStream in=gen.generateDiagram(bpmnmodel, "png",
        // activeActivityIds);
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(in, output);
    }

    @RequestMapping(value = "myleaves", method = RequestMethod.GET)
    String myleaves()
    {
        return "activiti/myleaves";
    }

    /**
     * 功能描述: 查看正在参与的流程(我发起的)
     * @auther: fuyuan
     * @date: 2019/12/30 0030 15:19
     */
    @RequestMapping(value = "setupprocess", method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<RunningProcess> setupprocess(HttpSession session, @RequestParam("current") int current,
            @RequestParam("rowCount") int rowCount)
    {
        int firstrow = (current - 1) * rowCount;
        String userid = (String) session.getAttribute("username");

        ProcessInstanceQuery query = runservice.createProcessInstanceQuery();
        int total = (int) query.count();

        // 结果: res.*
        // 	ACT_RU_EXECUTION RES
        //	INNER JOIN ACT_RE_PROCDEF P ON RES.PROC_DEF_ID_ = P.ID_

        // P.KEY_ = 'leave'   I.USER_ID_ = 'admin'

        List<ProcessInstance> a = query.processDefinitionKey("leave").involvedUser(userid).listPage(firstrow, rowCount);
        List<RunningProcess> list = new LinkedList<>();
        for (ProcessInstance p : a)
        {
            RunningProcess process = new RunningProcess();
            process.setActivityid(p.getActivityId());
            process.setBusinesskey(p.getBusinessKey());
            process.setExecutionid(p.getId());
            process.setProcessInstanceid(p.getProcessInstanceId());
            LeaveApply l = leaveservice.getleave(Integer.parseInt(p.getBusinessKey()));
            if (l.getUser_id().equals(userid))
                list.add(process);
            else
                continue;
        }
        DataGrid<RunningProcess> grid = new DataGrid<>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        grid.setTotal(total);
        grid.setRows(list);
        return grid;
    }




}
