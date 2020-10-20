package boot.spring.controller;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class test {

    @Autowired
    TaskService taskservice;

    @Autowired
    RuntimeService runtimeservice;

    @Autowired
    RuntimeService runservice;
    /**
     * 功能描述: 领取任务
     * @auther: fuyuan
     * @date: 2020/4/15 0015 14:50
     */
    @RequestMapping(value = "/getWork")
    public String getWork(String taskId,String userId){
        // 指定人 领取任务
        taskservice.claim(taskId, userId);
        return  "getWork";
    }

    /**
     * 功能描述: 完成任务
     * @auther: fuyuan
     * @date: 2020/4/15 0015 14:50
     */
    @RequestMapping(value = "/completeWork")
    public String completeWork(String taskId,String deptleaderapprove,String hrapprove){
        // 指定人 完成任务
        Map<String, Object> map = new HashMap<>();

        map.put("deptleaderapprove",deptleaderapprove);
        map.put("hrapprove",hrapprove);

        taskservice.complete(taskId, map);

        return  "completeWork";
    }

    // 指定组查询
       @RequestMapping(value = "/test1")
     public void test1(String name){
        List<Task> taskList = taskservice //获取任务service
                .createTaskQuery()//创建查询对象
                .taskCandidateUser(name)//参与者，组任务查询
                .list();
        if(taskList.size() > 0){
            for (Task task : taskList) {
                System.out.println("代办任务ID:"+task.getId());
                System.out.println("代办任务name:"+task.getName());
                System.out.println("代办任务创建时间:"+task.getCreateTime());
                System.out.println("代办任务办理人:"+task.getAssignee());
                System.out.println("流程实例ID:"+task.getProcessInstanceId());
                System.out.println("执行对象ID:"+task.getExecutionId());
            }
        }
    }

    // 指定人查询
    @RequestMapping(value = "/test2")
    public void test2(String name){
        List<Task> taskList = taskservice //获取任务service
                .createTaskQuery()//创建查询对象
                .taskAssignee(name)//指定查询人
                .list();
        if(taskList.size() > 0){
            for (Task task : taskList) {
                System.out.println("代办任务ID:"+task.getId());
                System.out.println("代办任务name:"+task.getName());
                System.out.println("代办任务创建时间:"+task.getCreateTime());
                System.out.println("代办任务办理人:"+task.getAssignee());
                System.out.println("流程实例ID:"+task.getProcessInstanceId());
                System.out.println("执行对象ID:"+task.getExecutionId());
            }
        }
    }

    // 指定组2查询
    @RequestMapping(value = "/test5")
    public void test5(String name){
        List<Task> taskList = taskservice //获取任务service
                .createTaskQuery()//创建查询对象
                .taskCandidateGroup(name)
                .list();
            if(taskList.size() > 0){
            for (Task task : taskList) {
                System.out.println("代办任务ID:"+task.getId());
                System.out.println("代办任务name:"+task.getName());
                System.out.println("代办任务创建时间:"+task.getCreateTime());
                System.out.println("代办任务办理人:"+task.getAssignee());
                System.out.println("流程实例ID:"+task.getProcessInstanceId());
                System.out.println("执行对象ID:"+task.getExecutionId());
            }
        }
    }

   // 启动流程
    @RequestMapping(value = "/GOGO")
   public void GOGO (String businesskey,String userId){
       // 制定组任务-人
       Map<String, Object> variables = new HashMap<>();
       variables.put("userId", userId);

       ProcessInstance instance = runtimeservice.startProcessInstanceByKey("test2", businesskey, variables);
       String instanceid = instance.getId();

       System.err.println("businesskey----业务id"+ businesskey);
       System.err.println("instanceid实例id----"+instanceid);
       System.err.println("variables----"+variables);
   }


    // 任务id查询业务主键
    @RequestMapping(value = "/test4")
   public Object test4(String taskId){
       Task task = taskservice.createTaskQuery().taskId(taskId).singleResult();
       ProcessInstance process = runservice.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId())
               .singleResult();

       return process.getBusinessKey();
   }

}
