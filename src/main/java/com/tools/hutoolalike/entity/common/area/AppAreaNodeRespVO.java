package com.tools.hutoolalike.entity.common.area;

import lombok.Data;

import java.util.List;

@Data
public class AppAreaNodeRespVO {

    private Integer id;

    private String name;

    /**
     * 子节点
     */
    private List<AppAreaNodeRespVO> children;

}
