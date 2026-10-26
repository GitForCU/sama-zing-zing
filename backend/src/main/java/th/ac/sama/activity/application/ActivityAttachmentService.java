package th.ac.sama.activity.application;

import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import th.ac.sama.activity.api.AttachmentInfo;
import th.ac.sama.activity.domain.*;
import th.ac.sama.activity.infrastructure.*;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.shared.error.BusinessException;

@Service
@Transactional
public class ActivityAttachmentService {
  public record UploadResult(AttachmentInfo attachment, long activityVersion) {}

  public record VersionResult(long activityVersion) {}

  public record Download(String name, String contentType, byte[] bytes) {}

  private final ActivityRepository activities;
  private final AttachmentRepository attachments;
  private final ActivityService activityService;
  private final CurrentUser current;
  private final EvidenceFilePolicy files;

  public ActivityAttachmentService(
      ActivityRepository activities,
      AttachmentRepository attachments,
      ActivityService activityService,
      CurrentUser current,
      EvidenceFilePolicy files) {
    this.activities = activities;
    this.attachments = attachments;
    this.activityService = activityService;
    this.current = current;
    this.files = files;
  }

  @Transactional(readOnly = true)
  public List<AttachmentInfo> list(UUID activityId) {
    activityService.requireReadable(activityId);
    return attachments.metadata(activityId);
  }

  public UploadResult upload(
      UUID activityId, long expectedVersion, UUID clientUploadId, MultipartFile file) {
    var student = current.student();
    var activity =
        activities.findForAttachmentUpdate(activityId).orElseThrow(BusinessException::missing);
    activity.requireEditable(student.getId());
    if (file.getSize() > EvidenceFilePolicy.MAX_BYTES)
      throw new BusinessException(413, "ไฟล์หลักฐานต้องมีขนาดไม่เกิน 5 MB ต่อไฟล์");
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw BusinessException.invalid("อ่านไฟล์หลักฐานไม่สำเร็จ กรุณาเลือกไฟล์ใหม่");
    }
    var validated = files.validate(file.getOriginalFilename(), bytes);
    var previous = attachments.findByActivityIdAndClientUploadId(activityId, clientUploadId);
    if (previous.isPresent()) {
      if (!previous.get().getSha256().equals(validated.sha256())
          || !previous.get().getOriginalName().equals(validated.name()))
        throw BusinessException.conflict("รหัสไฟล์นี้ถูกใช้กับหลักฐานอื่นแล้ว กรุณาเลือกไฟล์ใหม่");
      return new UploadResult(AttachmentInfo.from(previous.get()), activity.getVersion());
    }
    ActivityService.checkVersion(activity, expectedVersion);
    if (attachments.countByActivityId(activityId) >= EvidenceFilePolicy.MAX_FILES)
      throw BusinessException.invalid("แนบหลักฐานได้สูงสุด 5 ไฟล์ต่อกิจกรรม");
    var attachment =
        new ActivityAttachment(
            activity,
            clientUploadId,
            validated.name(),
            validated.contentType(),
            bytes,
            validated.sha256());
    attachments.save(attachment);
    activity.attachmentsChanged(student.getId());
    activities.flush();
    return new UploadResult(AttachmentInfo.from(attachment), activity.getVersion());
  }

  public VersionResult remove(UUID activityId, UUID attachmentId, long expectedVersion) {
    var student = current.student();
    var activity =
        activities.findForAttachmentUpdate(activityId).orElseThrow(BusinessException::missing);
    activity.requireEditable(student.getId());
    var file = attachments.findById(attachmentId);
    if (file.isEmpty()) return new VersionResult(activity.getVersion());
    if (!file.get().getActivity().getId().equals(activityId)) throw BusinessException.missing();
    ActivityService.checkVersion(activity, expectedVersion);
    attachments.delete(file.get());
    activity.attachmentsChanged(student.getId());
    activities.flush();
    return new VersionResult(activity.getVersion());
  }

  @Transactional(readOnly = true)
  public Download download(UUID activityId, UUID attachmentId) {
    activityService.requireReadable(activityId);
    var file =
        attachments
            .findByIdAndActivityId(attachmentId, activityId)
            .orElseThrow(BusinessException::missing);
    return new Download(file.getOriginalName(), file.getContentType(), file.getContent());
  }
}
