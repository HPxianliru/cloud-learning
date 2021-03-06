package com.tangyh.lamp.file.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tangyh.basic.annotation.log.SysLog;
import com.tangyh.basic.annotation.security.PreAuth;
import com.tangyh.basic.base.R;
import com.tangyh.basic.base.controller.DeleteController;
import com.tangyh.basic.base.controller.QueryController;
import com.tangyh.basic.base.controller.SuperSimpleController;
import com.tangyh.basic.base.request.PageParams;
import com.tangyh.basic.utils.BizAssert;
import com.tangyh.lamp.file.dto.AttachmentGetVO;
import com.tangyh.lamp.file.dto.AttachmentRemoveDTO;
import com.tangyh.lamp.file.dto.AttachmentResultDTO;
import com.tangyh.lamp.file.dto.AttachmentUploadVO;
import com.tangyh.lamp.file.dto.FilePageReqDTO;
import com.tangyh.lamp.file.entity.Attachment;
import com.tangyh.lamp.file.service.AttachmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.tangyh.basic.exception.code.ExceptionCode.BASE_VALID_PARAM;
import static com.tangyh.lamp.common.constant.SwaggerConstants.DATA_TYPE_MULTIPART_FILE;
import static com.tangyh.lamp.common.constant.SwaggerConstants.DATA_TYPE_STRING;
import static com.tangyh.lamp.common.constant.SwaggerConstants.PARAM_TYPE_QUERY;

/**
 * <p>
 * ????????? ???????????????
 * </p>
 *
 * @author zuihou
 * @since 2019-04-29
 */
@RestController
@RequestMapping("/attachment")
@Slf4j
@Api(value = "??????", tags = "??????")
@Validated
@SysLog(enabled = false)
@PreAuth(replace = "file:attachment:")
public class AttachmentController extends SuperSimpleController<AttachmentService, Attachment>
        implements QueryController<Attachment, Long, FilePageReqDTO>, DeleteController<Attachment, Long> {

    @Override
    public IPage<Attachment> query(PageParams<FilePageReqDTO> params) {
        IPage<Attachment> page = params.buildPage();
        baseService.page(page, params.getModel());
        return page;
    }

    @Override
    public R<Boolean> handlerDelete(List<Long> ids) {
        return R.success(baseService.remove(ids));
    }

    /**
     * ????????????
     */
    @ApiOperation(value = "????????????", notes = "????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "??????", dataType = DATA_TYPE_MULTIPART_FILE, allowMultiple = true, required = true),
    })
    @PostMapping(value = "/upload")
    @SysLog("????????????")
    @PreAuth("hasAnyPermission('{}add')")
    public R<Attachment> upload(@RequestParam(value = "file") MultipartFile file, @Validated AttachmentUploadVO attachmentVO) {
        // ??????????????????,?????????????????????
        if (file.isEmpty()) {
            return R.fail(BASE_VALID_PARAM.build("?????????????????????????????????????????????"));
        }
        return R.success(baseService.upload(file, attachmentVO));
    }


    @ApiOperation(value = "???????????????????????????id????????????", notes = "???????????????????????????id????????????")
    @DeleteMapping(value = "/biz")
    @SysLog("??????????????????????????????")
    @PreAuth("hasAnyPermission('{}delete')")
    public R<Boolean> removeByBizIdAndBizType(@RequestBody AttachmentRemoveDTO dto) {
        return R.success(baseService.removeByBizIdAndBizType(dto.getBizId(), dto.getBizType()));
    }

    @ApiOperation(value = "????????????", notes = "????????????")
    @ApiResponses(
            @ApiResponse(code = 60103, message = "??????id??????")
    )
    @GetMapping
    @SysLog("??????????????????????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bizIds", value = "??????id", dataType = DATA_TYPE_STRING, allowMultiple = true, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = "bizTypes", value = "????????????", dataType = DATA_TYPE_STRING, allowMultiple = true, paramType = PARAM_TYPE_QUERY),
    })
    @PreAuth("hasAnyPermission('{}view')")
    public R<List<AttachmentResultDTO>> findAttachment(@RequestParam(value = "bizTypes", required = false) String[] bizTypes,
                                                       @RequestParam(value = "bizIds", required = false) String[] bizIds) {
        //??????????????????
        BizAssert.isTrue(!(ArrayUtils.isEmpty(bizTypes) && ArrayUtils.isEmpty(bizIds)), BASE_VALID_PARAM.build("????????????????????????"));
        return R.success(baseService.find(bizTypes, bizIds));
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param ids ??????id
     */
    @ApiOperation(value = "????????????id????????????", notes = "????????????id???????????????????????????")
    @PostMapping(value = "/downloadByIds", produces = "application/octet-stream")
    @SysLog("????????????")
    @PreAuth("hasAnyPermission('{}download')")
    public void download(@RequestBody Long[] ids,
                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        BizAssert.isTrue(ArrayUtils.isNotEmpty(ids), BASE_VALID_PARAM.build("??????id????????????"));
        baseService.download(request, response, ids);
    }

    /**
     * ??????????????????????????????id?????????????????????2???????????????????????????
     *
     * @param bizIds   ??????id
     * @param bizTypes ????????????
     * @author zuihou
     * @date 2019-05-12 21:23
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bizIds[]", value = "??????id??????", dataType = DATA_TYPE_STRING, allowMultiple = true, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = "bizTypes[]", value = "??????????????????", dataType = DATA_TYPE_STRING, allowMultiple = true, paramType = PARAM_TYPE_QUERY),
    })
    @ApiOperation(value = "??????????????????/??????id????????????", notes = "????????????id?????????????????????????????????????????????")
    @GetMapping(value = "/downloadByBiz", produces = "application/octet-stream")
    @SysLog("??????????????????????????????")
    @PreAuth("hasAnyPermission('{}download')")
    public void downloadByBiz(
            @RequestParam(value = "bizIds[]", required = false) String[] bizIds,
            @RequestParam(value = "bizTypes[]", required = false) String[] bizTypes,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        BizAssert.isTrue(!(ArrayUtils.isEmpty(bizTypes) && ArrayUtils.isEmpty(bizIds)), BASE_VALID_PARAM.build("????????????id?????????????????????????????????"));
        baseService.downloadByBiz(request, response, bizTypes, bizIds);
    }

    /**
     * ??????????????????????????????
     *
     * @param url      ????????????
     * @param filename ????????????
     * @author zuihou
     * @date 2019-05-12 21:24
     */
    @ApiOperation(value = "??????url????????????(?????????)", notes = "???????????????url????????????(??????????????????????????????url????????????????????????nginx)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "url", value = "??????url", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
            @ApiImplicitParam(name = "filename", value = "?????????", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    })
    @GetMapping(value = "/downloadByUrl", produces = "application/octet-stream")
    @SysLog("??????????????????????????????")
    @PreAuth("hasAnyPermission('{}download')")
    public void downloadUrl(@RequestParam(value = "url") String url, @RequestParam(value = "filename", required = false) String filename,
                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        BizAssert.isTrue(StrUtil.isNotEmpty(url), BASE_VALID_PARAM.build("??????????????????????????????"));
        log.info("name={}, url={}", filename, url);
        baseService.downloadByUrl(request, response, url, filename);
    }


    /**
     * ??????????????????????????????
     *
     * @param path  ??????????????????
     * @param group ???
     * @author zuihou
     * @date 2019-05-12 21:24
     */
    @ApiOperation(value = "????????????path????????????", notes = "????????????path????????????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "path", value = "??????????????????", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, required = true),
            @ApiImplicitParam(name = "group", value = "???", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    })
    @GetMapping(value = "/downloadByPath", produces = "application/octet-stream")
    @SysLog("????????????path????????????")
    @PreAuth("hasAnyPermission('{}download')")
    public void downloadByPath(
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "path") String path,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        baseService.downloadByPath(request, response, group, path);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param paths ????????????
     */
    @ApiOperation(value = "???????????????????????????????????????????????????", notes = "???????????????????????????????????????????????????")
    @PostMapping(value = "/getUrls")
    public R<List<String>> getUrls(@RequestBody List<AttachmentGetVO> paths) {
        return R.success(baseService.getUrls(paths, 172800));
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param path   ????????????
     * @param expiry ?????????
     */
    @ApiOperation(value = "?????????????????????????????????????????????", notes = "?????????????????????????????????????????????")
    @GetMapping(value = "/getUrl")
    public R<String> getUrl(
            @ApiParam(name = "group", value = "group")
            @RequestParam(value = "group", required = false) String group,
            @ApiParam(name = "path", value = "????????????")
            @RequestParam(value = "path") String path,
            @ApiParam(name = "expiry", value = "????????????")
            @RequestParam(value = "expiry", defaultValue = "172800") Integer expiry) {
        return R.success(baseService.getUrl(group, path, expiry));
    }
}
