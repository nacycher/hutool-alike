package com.tools.hutoolalike.controller.common;

import cn.hutool.core.lang.Assert;
import com.tools.hutoolalike.entity.common.CommonResult;
import com.tools.hutoolalike.entity.common.area.AppAreaNodeRespVO;
import com.tools.hutoolalike.entity.common.area.Area;
import com.tools.hutoolalike.utils.AreaUtils;
import com.tools.hutoolalike.utils.BeanUtils;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tools.hutoolalike.entity.common.CommonResult.success;

@RestController
@RequestMapping("/common")
public class CommonController {

    @GetMapping("/tree")
    @PermitAll
    public CommonResult<List<AppAreaNodeRespVO>> getAreaTree() {
        Area area = AreaUtils.getArea(Area.ID_CHINA);
        Assert.notNull(area, "获取不到中国");
        return success(BeanUtils.toBean(area.getChildren(), AppAreaNodeRespVO.class));
    }
}
