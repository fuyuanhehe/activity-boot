package boot.spring.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import boot.spring.pagemodel.MSG;
import boot.spring.service.LoginService;
import io.swagger.annotations.Api;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Api(value = "登录登出接口")
@Controller
public class Login
{
    @Autowired
    LoginService loginservice;

    @RequestMapping(value = "/loginvalidate")
    public String loginvalidate(@RequestParam("username") String username, @RequestParam("password") String pwd,
            HttpSession httpSession)
    {
        if (username == null)
            return "login";
        String realpwd = loginservice.getpwdbyname(username);
        if (realpwd != null && pwd.equals(realpwd))
        {
            httpSession.setAttribute("username", username);
            return "index";
        } else
            return "fail";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login()
    {
        return "login";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpSession httpSession)
    {
        httpSession.removeAttribute("username");
        return "login";
    }

    @RequestMapping(value = "/currentuser", method = RequestMethod.GET)
@ResponseBody
public MSG currentuser(HttpSession httpSession)
{
    String userid = (String) httpSession.getAttribute("username");
    return new MSG(userid);
}

/*    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public String test(HttpSession session){
        session.setAttribute("name", "aabbcc");
        return "ok";
    }*/

 /*   @RequestMapping(value = "/test2", method = RequestMethod.GET)
    @ResponseBody
    public String test2(HttpSession session){
        return session.getAttribute("name").toString();
    }
*/
/*    @RequestMapping(value = "/test3", method = RequestMethod.GET)
    @ResponseBody
    public String test3( ){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        HttpSession session = request.getSession();

        session.setAttribute("name1", "hhhhh");
        return "ok";
    }*/
/*
    @RequestMapping(value = "/test4", method = RequestMethod.GET)
    @ResponseBody
    public String test4( ){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        HttpSession session = request.getSession();

        return    session.getAttribute("name1").toString();
    }*/

}
