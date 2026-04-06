package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.Attachment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 任勇林
 * @since 2024-01-12
 */
public interface AttachmentService extends IService<Attachment> {
    boolean delete(Integer[] idArr);
    boolean uploadAttach(MultipartFile[] files, String formValue);

}
