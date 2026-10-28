package com.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分页注解
 * 加在接口上，自动完成分页参数的转换
 * 1、 注解加在API接口上，PagerAspect切面自动拦截处理
 * 2、 如果参数类继承 QueryDTO（pojo/dto包下）基础查询条件类，且携带分页参数（current当前页、size页面大小），自动完成分页参数转换
 * 3、 转换当前页公式：（当前页 - 1）*页面大小
 * 4、 最终进SQL的 LIMIT offset,count 实现数据翻页
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Pager {

}