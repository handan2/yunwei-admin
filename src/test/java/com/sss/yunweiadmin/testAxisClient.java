package com.sss.yunweiadmin;

import cn.hutool.core.util.XmlUtil;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.xpath.XPathConstants;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.apache.axis.handlers.soap.SOAPService;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class testAxisClient {
    //测试反射/获得所有属性名
    @Test
    public void testReflect() throws Exception{
        Class<?> clazz = Class.forName("com.sss.yunweiadmin.model.excel.AsComputerDownload");
        System.out.println("===============本类属性===============");
        // 取得本类的全部属性
        Field[] field = clazz.getDeclaredFields();
        for (int i = 0; i < field.length; i++) {
            // 权限修饰符
            int mo = field[i].getModifiers();
            String priv = Modifier.toString(mo);
            // 属性类型
            Class<?> type = field[i].getType();
            System.out.println(priv + " " + type.getName() + " " + field[i].getName() + ";");
        }
    }

    //测试解析xml
    @Test
    public void random() {
        String baomiNo = "0094";
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyHHmmssSSS");
        String dateStr = date.format(fmt);
        Random rnd = new Random();
        int code = rnd.nextInt(1000) + 100;
        String randomNum = Integer.toString(code);

            baomiNo = baomiNo + "D";

        baomiNo = baomiNo + dateStr + randomNum;
        System.out.println(baomiNo);
    }
   //测试解析xml
    @Test
    public void abc2(){

        String xml = "<root><a name = \"第一个元素\"><b>最底层节点值</b></a></root>";
        String xml1 = "<root><data><msg>1</msg></data></root>";
        Document document = XmlUtil.parseXml(xml1);
        Object msgString = XmlUtil.getByXPath("//root/data/msg", document, XPathConstants.STRING);
        System.out.println("msg值："+ msgString);
//        Element goalElement = XmlUtil.getElementByXPath("//root/a",document);//找不到时为null
//        Object bString = XmlUtil.getByXPath("//root/a/b", document, XPathConstants.STRING);//找不到时为“”
//
//        System.out.println("b元素节点值："+bString);
//        String name = goalElement.getAttribute("name");
//        System.out.println("a元素属性值："+name);

    }

    @Test
    public void abc1() throws ServiceException,RemoteException{

        // 调用WebService SayHello
        HelloServiceImplServiceLocator services = new HelloServiceImplServiceLocator();
        HelloServiceImplServiceSoapBindingStub Hello = (HelloServiceImplServiceSoapBindingStub) services
                .getHelloServiceImplPort();
        System.out.println(Hello.sayHello("xxx"));
    }
    @Test
    public void abc(){
        System.out.println("testAxis Start......");
        try {
            String result = "";
            Service service=new Service();
            Call call=(Call)service.createCall();
            call.setTargetEndpointAddress("http://localhost:8000/ws11/hello");

            /*设置入口参数*/
            call.addParameter(new QName("http://service1.itheima.com/","aaa"), XMLType.XSD_STRING, ParameterMode.IN);
            call.setReturnType(XMLType.XSD_STRING);
          //  call.setUseSOAPAction(true);
           // call.setSOAPActionURI("");//20220313
//            call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//            call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//            call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);

           //call.setOperationName(new QName("http://service1.itheima.com/","sayHello"));//设置操作名
            call.setOperationName(new QName("http://service1.itheima.com/","sayHello"));
          //  call.setOperationName("sayHello");

            //call.setUseSOAPAction(true);
           // call.setSOAPActionURI("http://service1.itheima.com/sayHello");

             //20220328下面这三行加不加都可以
//            SOAPService soap = new SOAPService();
//            soap.setName("HelloServiceImpl");
//            call.setSOAPService(soap);


            String[] param={"common"};
            //.invoke(new java.lang.Object[] {aaa})
           // result = (String) call.invoke(param);
            Object obj = call.invoke( param);
            System.out.println(obj );

        }catch (Exception e) {
            System.out.println("=========访问webservice："+e.getMessage());
        }

    }




}
