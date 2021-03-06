package com.model;

import com.util.StoreType;
import com.util.UserState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "store")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    private String id;//商户7位识别码

    private String name;//店铺名字

    private String boss;//法人代表

    private String password;//登录密码

    private String email;//联系方式

    private String introduce;//店铺简介

    @Enumerated(EnumType.STRING)
    private StoreType type;//店铺类型

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    private List<String> food_types;//食物种类

    //地址
    private String province;//省
    private String city;//市
    private String area;//区
    private String detail;//具体地址

    @Enumerated(EnumType.STRING)
    private UserState state;//店铺状态，审核中/已通过/信息更改中

    @ElementCollection(targetClass = Integer.class)
    @MapKeyColumn(name = "standard")
    private Map<Integer,Integer> discounts;//满减优惠

    @OneToMany(targetEntity = Good.class,cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Good> goods;//商品
}
