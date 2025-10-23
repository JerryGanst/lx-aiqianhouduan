package org.example.ai_api.Service;

import org.example.ai_api.Bean.ApiRepeat.ResumeRepeat;
import org.example.ai_api.Bean.ApiRepeat.ResumeTaskRepeat;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Entity.ResumeTask;
import org.example.ai_api.Bean.WebRequest.Resume;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.ResumeTaskDao;
import org.example.ai_api.Persistence.Repository.FileUploadInfoRepository;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    @Autowired
    private ResumeTaskDao resumeTaskDao;
    @Autowired
    private FileUploadInfoRepository fileUploadInfoRepository;

    //保存简历任务
    public ResumeTask saveResumeTask(ResumeTask resumeTask) {
        if (resumeTask == null) {
            throw new BadRequestException("任务不能为空");
        }
        if(resumeTask.getId() != null && !resumeTask.getId().isEmpty()){
            //获取数据库中任务
            ResumeTask dbResumeTask = resumeTaskDao.findById(resumeTask.getId());
            if(dbResumeTask != null){
                //更新任务
                dbResumeTask.setResumeTaskRepeat(resumeTask.getResumeTaskRepeat());
                dbResumeTask.setComplete(resumeTask.getResumeTaskRepeat().isTaskCompleted());
                dbResumeTask.setLastUpdateTime(Utils.getNowDate());
                return resumeTaskDao.save(dbResumeTask);
            }else {
                //任务不存在，抛出异常
                throw new NotFoundException("任务不存在");
            }
        }
        //保存新任务
        resumeTask.setId(null);
        resumeTask.setCreateTime(Utils.getNowDate());
        resumeTask.setLastUpdateTime(Utils.getNowDate());
        resumeTask.setComplete(false);
        return resumeTaskDao.save(resumeTask);
    }

    //启动任务后，预保存任务
    public ResumeTask saveResumeTask(Resume resume, ResumeRepeat repeat) {
        FileUpload JdFile = null;
        if(resume.getJdFile() != null && !resume.getJdFile().isEmpty()){
            JdFile =  fileUploadInfoRepository.findById(resume.getJdFile())
                    .orElseThrow(() -> new NotFoundException("文件不存在"));
        }
        List<FileUpload> resumeFiles = resume.getResumeFileIds().stream()
                .map(
                        resumeFile -> fileUploadInfoRepository.findById(resumeFile)
                                .orElseThrow(() -> new NotFoundException("文件不存在"))
                )
                .collect(Collectors.toList());
        ResumeTask resumeTask = creatResumeTask(resume, repeat, JdFile, resumeFiles);
        return saveResumeTask(resumeTask);
    }

    //更新简历任务
    public ResumeTask updateResumeTask(ResumeTaskRepeat repeat) {
        if (repeat == null) {
            throw new BadRequestException("任务结果不能为空");
        }
        ResumeTask resumeTask = resumeTaskDao.findByBatchId(repeat.getTaskJson().getUuid());
        if (resumeTask != null) {
            resumeTask.setComplete(true);
            resumeTask.setResumeTaskRepeat(repeat);
            resumeTaskDao.save(resumeTask);
        }
        return resumeTask;
    }

    //获取简历任务
    public ResumeTask getResumeTaskById(String id) {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("任务id不能为空");
        }
        ResumeTask resumeTask = resumeTaskDao.findById(id);
        if (resumeTask == null) {
            throw new NotFoundException("任务不存在");
        }
        return resumeTask;
    }

    //根据用户id获取简历任务列表
    public List<ResumeTask> getResumeTaskByUserId(String userId, String keyword) {
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户id不能为空");
        }
        return resumeTaskDao.findByUserId(userId, keyword);
    }

    //删除简历任务
    public void deleteResumeTaskById(String id) {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("任务id不能为空");
        }
        resumeTaskDao.deleteById(id);
    }

    //修改简历任务标题
    public void changeResumeTaskTitleById(String id, String title) {
        ResumeTask resumeTask = resumeTaskDao.findById(id);
        if (resumeTask == null) {
            throw new NotFoundException("任务不存在");
        }
        if (title.equals(resumeTask.getTitle())) {
            return;
        }
        List<ResumeTask> resumeTasks = resumeTaskDao.findByUserIdAndTitle(resumeTask.getUserId(), title);
        if (resumeTasks != null && !resumeTasks.isEmpty()) {
            throw new BadRequestException("该用户已存在相同标题的任务");
        }
        resumeTask.setTitle(title);
        resumeTask.setLastUpdateTime(Utils.getNowDate());
        resumeTaskDao.save(resumeTask);
    }

    private ResumeTask creatResumeTask(Resume resume, ResumeRepeat repeat, FileUpload JdFile, List<FileUpload> resumeFiles) {
        ResumeTask resumeTask = new ResumeTask();
        String title;
        if (resume.getJdText() == null || resume.getJdText().isEmpty()) {
            title = JdFile.getFileName();
        } else {
            title = resume.getJdText();
        }
        resumeTask.setUserId(resume.getUserId());
        resumeTask.setText(resume.getJdText() ==  null ? "" : resume.getJdText());
        resumeTask.setJDFile(JdFile);
        resumeTask.setResumeFiles(resumeFiles);
        resumeTask.setTitle(title);
        resumeTask.setComplete(false);
        resumeTask.setCreateTime(Utils.getNowDate());
        resumeTask.setLastUpdateTime(Utils.getNowDate());
        resumeTask.setResumeRepeat(repeat);
        return resumeTask;
    }
}
