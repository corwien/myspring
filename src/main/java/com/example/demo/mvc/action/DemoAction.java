package com.example.demo.mvc.action;

import com.example.demo.service.IDemoService;
import com.example.mvcframework.annotation.Autowired;
import com.example.mvcframework.annotation.Controller;
import com.example.mvcframework.annotation.RequestMapping;
import com.example.mvcframework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author: kaiyi
 * @create: 2020-10-14 12:48
 */
@Controller
@RequestMapping("/demo")
public class DemoAction {

  @Autowired
  private IDemoService demoService;

  @RequestMapping("/query")
  public  void query(HttpServletRequest req, HttpServletResponse resp, @RequestParam("name") String name){
    String result = demoService.get(name);
    try {
      resp.getWriter().write(result);
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  @RequestMapping("/add")
  public  void add(HttpServletRequest req, HttpServletResponse resp,  @RequestParam("a")Integer a,  @RequestParam("b") Integer b){
    try {
      resp.getWriter().write(a + "+" + b + "=" + (a+b));
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  @RequestMapping("/remove")
  public  void remove(HttpServletRequest req, HttpServletResponse resp,  @RequestParam("id")Integer id){

  }
}
