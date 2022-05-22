//定义模块
layui.define(["jquery",'layer'], function (exports) {

    // var form = layui.form; //只有执行了这一步，部分表单元素才会自动修饰成功
    var $ = layui.$;
    var layer = layui.layer;

    var obj = {

        httpPost: function (apiUrl, postData, callback) {
            var fullUrl = "http://" + window.location.host + "/cms" + apiUrl + "?" + "timestamp=" + new Date().getTime();
            $.ajax({
                url: fullUrl,
                type: 'post',
                data: postData,
                dataType: 'json',
                xhrFields: {
                    withCredentials: true
                },
                success: function (resp) {
                    if(resp.msg && resp.msg=="无权限操作"){
                        window.top.location.href='/login.html';
                    }else{
                        callback(resp);
                    }
                }
            })
        },

        syncHttpPost: function (apiUrl, postData) {
            var fullUrl = "http://" + window.location.host + "/cms" + apiUrl + "?" + "timestamp=" + new Date().getTime();

            return new Promise(function (resolve, reject) {
                $.ajax({
                    url: fullUrl,
                    type: 'post',
                    data: postData,
                    dataType: 'json',
                    xhrFields: {
                        withCredentials: true
                    },
                    success: function (resp) {
                        if(resp.msg && resp.msg=="无权限操作"){
                            window.location.href='./login.html';
                            reject(resp)
                        }else{
                            resolve(resp);
                        }
                    },
                    err:function (resp) {
                        reject(resp)
                    }
                })
            })

        },

        httpUploadFile: function (fileInfo, onprogress) {
            var formData = new FormData();
            // formData.append("name", fileInfo.name);
            // formData.append("size", fileInfo.size);
            formData.append("upload_files", fileInfo);
            var fullUrl = "http://" + window.location.host + "/cms" + '/file/user_upload'
            $.ajax({
                url: fullUrl,
                type: 'post',
                data: formData,
                cache: false,
                processData: false,
                contentType: false,
                xhr: function () {
                    var xhr = $.ajaxSettings.xhr();
                    if (onprogress && xhr.upload) {
                        xhr.upload.addEventListener("progress", onprogress, false);
                        return xhr;
                    }
                }
            })
        },
        httpDownloadFile:function(apiUrl,postData){
            // 获取XMLHttpRequest
            var fullUrl = "http://" + window.location.host + "/cms" + apiUrl ;
            var xmlResquest = new XMLHttpRequest();
            //  发起请求
            xmlResquest.open("POST", fullUrl, true);
            // 设置请求头类型
            xmlResquest.setRequestHeader("Content-type", "application/json");
            xmlResquest.setRequestHeader("postData",postData);
            xmlResquest.responseType = "blob";

            var loadingFlag = layer.msg('下载中，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
            xmlResquest.addEventListener("progress", function(ev) {
                // 下载中事件：计算下载进度
                if(ev.total==ev.loaded){
                    layer.close(loadingFlag)
                    layer.msg("下载成功!",{icon:1})
                }
            });
            //  返回
            xmlResquest.onload = function(oEvent) {
                //alert(this.status);
                var content = xmlResquest.response;
                // 组装a标签
                var elink = document.createElement("a");
                var name = xmlResquest.getResponseHeader("Content-disposition");
                //设置文件下载路径
                if(!name){
                    layer.close(loadingFlag)
                    layer.msg("下载失败,相关文件可能不存在!",{icon:2})
                }else{
                    elink.download = name.split("=")[1] ;
                    elink.style.display = "none";
                    var blob = new Blob([content]);

                    //解决下载不存在文件的问题，根据blob大小判断
                    if(blob.size==0){
                        layer.msg('服务器没找到此文件，请联系管理员!');
                    }else{
                        elink.href = URL.createObjectURL(blob);
                        document.body.appendChild(elink);
                        elink.click();
                        document.body.removeChild(elink);
                    }
                }
            };
            xmlResquest.send();
        },

        //带callback就是异步，不带callback则是同步
        //用户管理相关接口
        userLogin: function (postData, callback) {
            //alert("user loing!" + postData)
            if(callback){
                this.httpPost("/account/userlogin", postData, callback)
            }else {
                return this.syncHttpPost("/account/userlogin", postData)
            }
        },

        userLogout:function(postData, callback){
            if(callback){
                this.httpPost("/account/logout", postData, callback)
            }else {
                return this.syncHttpPost("/account/logout", postData)
            }
        },

        getUserList:function(postData, callback){
            if(callback){
                this.httpPost("/account/allUserList", postData, callback)
            }else {
                return this.syncHttpPost("/account/allUserList", postData)
            }
        },

        getUserInfo:function(postData, callback){
            if(callback){
                this.httpPost("/account/getUserInfo", postData, callback)
            }else {
                return this.syncHttpPost("/account/getUserInfo", postData)
            }
        },

        userModifyPwd:function(postData, callback){
            if(callback){
                this.httpPost("/account/modifypwd", postData, callback)
            }else {
                return this.syncHttpPost("/account/modifypwd", postData)
            }
        },

        userModifyInfo:function(postData, callback){
            if(callback){
                this.httpPost("/account/mod_userinfo", postData, callback)
            }else {
                return this.syncHttpPost("/account/mod_userinfo", postData)
            }
        },

        addNewUser:function(postData, callback){
            if(callback){
                this.httpPost("/account/add_user", postData, callback)
            }else {
                return this.syncHttpPost("/account/add_user", postData)
            }
        },

        delUser:function(postData, callback){
            if(callback){
                this.httpPost("/account/del_user", postData, callback)
            }else {
                return this.syncHttpPost("/account/del_user", postData)
            }
        },
        getLocalIP:function(postData, callback){
            if(callback){
                this.httpPost("/account/getLocalIP", postData, callback)
            }else {
                return this.syncHttpPost("/account/getLocalIP", postData)
            }
        },

        //终端分组管理
        getTerminalGrp: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/group/list", postData, callback)
            }else {
                return  this.syncHttpPost("/terminal/group/list", postData)
            }
        },

        getAllTerminalOfUser: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/newList", postData, callback)
            }else {
                return  this.syncHttpPost("/terminal/newList", postData)
            }
        },


        addOrUpdateGroup: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/group/addOrUpdate", postData, callback)
            }else {
                return  this.syncHttpPost("/terminal/group/addOrUpdate", postData)
            }
        },
        delTerminalGroup: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/group/del", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/group/del", postData)
            }
        },

        changeGroup: function (postData, callback) {
            console.log("changeGroup:" + postData)
            if(callback){
                this.httpPost("/terminal/change_group", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/change_group", postData)
            }
        },
        getTerminalList: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/list", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/list", postData)
            }
        },
        changeTerminalVolume: function (postData, callback) {
            if(callback){
                this.httpPost("/terminal/change_volume", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/change_volume", postData)
            }
        },

        findTerminals:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/findTerminals", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/findTerminals", postData)
            }
        },

        terminalUpdateInfo:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/updateInfo", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/updateInfo", postData)
            }

        },

        delTerminal:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/del_dev", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/del_dev", postData)
            }
        },

        rebootTerminals:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/reboot_now", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/reboot_now", postData)
            }
        },


        //视频查看接口
        getTerminalVideo: function (postData, callback) {
            if(callback){
                this.httpPost("/video/realtime_url", postData, callback)
            }else {
                return this.syncHttpPost("/video/realtime_url", postData)
            }
        },
        //升级设置接口
        appUpdateStatus:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/appUpdateStatus", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/appUpdateStatus", postData)
            }
        },

        appUpdateNow:function(postData, callback){
            if(callback){
                this.httpPost("/terminal/appUpdateNow", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/appUpdateNow", postData)
            }
        },

        //外接摄像头接口

        getCameraList:function(postData, callback){
            if(callback){
                this.httpPost("/camera/list", postData, callback)
            }else {
                return this.syncHttpPost("/camera/list", postData)
            }
        },

        cameraAddorUpdate:function(postData, callback){
            if(callback){
                this.httpPost("/camera/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/camera/addOrUpdate", postData)
            }
        },

        cameraUnbind:function(postData, callback){
            if(callback){
                this.httpPost("/camera/delete", postData, callback)
            }else {
                return this.syncHttpPost("/camera/delete", postData)
            }
        },

        //文件操作接口
        getFileList: function (postData, callback) {
            if(callback){
                this.httpPost("/file/file_list", postData, callback)
            }else {
                return this.syncHttpPost("/file/file_list", postData)
            }
        },
        delFiles:function(postData, callback){
            if(callback){
                this.httpPost("/file/del_files", postData, callback)
            }else {
                return this.syncHttpPost("/file/del_files", postData)
            }

        },
        getDownLoadUrl:function(attach_id){
            return "http://" + window.location.host + "/cms/file/download/" + attach_id;
        },
        modifyFileName:function(postData, callback){
            if(callback){
                this.httpPost("/file/modify_name", postData, callback)
            }else {
                return this.syncHttpPost("/file/modify_name", postData)
            }
        },
        getPlayHelpVideUrl:function(help_id,video_name){
            return "http://" + window.location.host + "/cms/file/play/help_video?" + "help_id=" + help_id + "&video_name=" + video_name;

        },
        //普通任务相关接口
        getTaskList: function (postData, callback) {
            if(callback){
                this.httpPost("/task/task_list", postData, callback)
            }else {
                return this.syncHttpPost("/task/task_list", postData)
            }
        },
        getTaskByTaskId: function(postData, callback){
            if(callback){
                this.httpPost("/task/getTaskByTaskId", postData, callback)
            }else {
                return this.syncHttpPost("/task/getTaskByTaskId", postData)
            }
        },
        setTerminalsOfTask:function(postData, callback){
            if(callback){
                this.httpPost("/task/setReadyOrFailedTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/task/setReadyOrFailedTerminal", postData)
            }
        },
        addOrUpdateTask: function (postData, callback) {
            if(callback){
                this.httpPost("/task/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/task/addOrUpdate", postData)
            }
        },

        sendTask2terminal:function (postData,callback) {
            if(callback){
                this.httpPost("/task/send2terminal", postData, callback)
            }else {
                return this.syncHttpPost("/task/send2terminal", postData)
            }
        },

        stopTask:function(postData,callback){
            if(callback){
                this.httpPost("/task/stopTask", postData, callback)
            }else {
                return this.syncHttpPost("/task/stopTask", postData)
            }
        },

        sendTimeCountDownTask:function(postData,callback){
            if(callback){
                this.httpPost("/task/sendCountDownTime", postData, callback)
            }else {
                return this.syncHttpPost("/task/sendCountDownTime", postData)
            }

        },

        delTask:function (postData,callback) {
            if(callback){
                this.httpPost("/task/delTask", postData, callback)
            }else {
                return this.syncHttpPost("/task/delTask", postData)
            }
        },

        cleanTask:function(postData, callback){
            if(callback){
                this.httpPost("/task/clean", postData, callback)
            }else {
                return this.syncHttpPost("/task/clean", postData)
            }
        },

        getTerminalTask:function(postData, callback){
            if(callback){
                this.httpPost("/task/terminal_task", postData, callback)
            }else {
                return this.syncHttpPost("/task/terminal_task", postData)
            }
        },

        stopTerminalTask:function(postData, callback){
            if(callback){
                this.httpPost("/task/stopTerminalTask", postData, callback)
            }else {
                return this.syncHttpPost("/task/stopTerminalTask", postData)
            }
        },

        exportTask:function(postData,saveName){
            this.httpDownloadFile("/task/export", postData)
        },

        //屏蔽任务接口
        getShieldTasks:function ( postData, callback) {
            if(callback){
                this.httpPost("/shield/task_list", postData, callback)
            }else {
                return this.syncHttpPost("/shield/task_list", postData)
            }
        },
        getTaskByShieldId:function(postData, callback){
            if(callback){
                this.httpPost("/shield/getTaskByShieldId", postData, callback)
            }else {
                return this.syncHttpPost("/shield/getTaskByShieldId", postData)
            }
        },
        setShieldTerminals:function(postData, callback){
            if(callback){
                this.httpPost("/shield/setReadyOrFailedTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/shield/setReadyOrFailedTerminal", postData)
            }
        },
        addOrUpdateShieldTask:function ( postData, callback) {
            if(callback){
                this.httpPost("/shield/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/shield/addOrUpdate", postData)
            }
        },
        deleteShieldTask:function ( postData, callback) {
            if(callback){
                this.httpPost("/shield/delete", postData, callback)
            }else {
                return this.syncHttpPost("/shield/delete", postData)
            }
        },
        sendShielTask2Terminals:function ( postData, callback) {
            if(callback){
                this.httpPost("/shield/send2terminal", postData, callback)
            }else {
                return this.syncHttpPost("/shield/send2terminal", postData)
            }
        },
        clearShieldSetting:function(postData, callback){
            if(callback){
                this.httpPost("/shield/clearShieldSetting", postData, callback)
            }else {
                return this.syncHttpPost("/shield/clearShieldSetting", postData)
            }
        },
        startOrStopShieldForExam:function(postData, callback){
            if(callback){
                this.httpPost("/shield/startOrStopShieldForExam", postData, callback)
            }else {
                return this.syncHttpPost("/shield/startOrStopShieldForExam", postData)
            }
        },

        //日志列表接口
        getTerminalLog:function (postData, callback) {
            if(callback){
                this.httpPost("/log/terminal_log", postData, callback)
            }else {
                return this.syncHttpPost("/log/terminal_log", postData)
            }
        },

        getUpdateLog:function(postData,callback){
            if(callback){
                this.httpPost("/terminal/update_log", postData, callback)
            }else {
                return this.syncHttpPost("/terminal/update_log", postData)
            }
        },

        getUserLoginLog:function (postData, callback) {
            if(callback){
                this.httpPost("/log/loginlog", postData, callback)
            }else {
                return this.syncHttpPost("/log/loginlog", postData)
            }
        },

        getUserActionLog:function (postData, callback) {
            if(callback){
                this.httpPost("/log/userlog", postData, callback)
            }else {
                return this.syncHttpPost("/log/userlog", postData)
            }
        },

        //求助信息接口
        getTerminalHelpList:function (postData, callback) {
            if(callback){
                this.httpPost("/help/help_list", postData, callback)
            }else {
                return this.syncHttpPost("/help/help_list", postData)
            }
        },
        modifyHelpSatus:function (postData, callback) {
            if(callback){
                this.httpPost("/help/modifyHelpSatus", postData, callback)
            }else {
                return this.syncHttpPost("/help/modifyHelpSatus", postData)
            }
        },
        getHelpRecordVideoList:function (postData, callback) {
            if(callback){
                this.httpPost("/help/help_video_list", postData, callback)
            }else {
                return this.syncHttpPost("/help/help_video_list", postData)
            }
        },
        uploadHelpVideo:function (postData, callback) {
            if(callback){
                this.httpPost("/help/help_video", postData, callback)
            }else {
                return this.syncHttpPost("/help/help_video", postData)
            }
        },

        getExamList:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/exam_list", postData, callback)
            }else {
                return this.syncHttpPost("/exam/exam_list", postData)
            }
        },
        getExamInfoById:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/getExamInfoById", postData, callback)
            }else {
                return this.syncHttpPost("/exam/getExamInfoById", postData)
            }
        },
        examAddOrUpdate:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/exam/addOrUpdate", postData)
            }
        },
        delExamInfoAndTask:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/del_exam", postData, callback)
            }else {
                return this.syncHttpPost("/exam/del_exam", postData)
            }
        },
         //以下为考试模板接口
        getExamTpllist:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/getExamTpllist", postData, callback)
            }else {
                return this.syncHttpPost("/exam/getExamTpllist", postData)
            }
        },
        getTasktplByExamTplId:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/getTasktplByExamTplId", postData, callback)
            }else {
                return this.syncHttpPost("/exam/getTasktplByExamTplId", postData)
            }
        },
        createExamFromTplId:function (postData, callback) {
            if(callback){
                this.httpPost("/exam/createExamFromTplId", postData, callback)
            }else {
                return this.syncHttpPost("/exam/createExamFromTplId", postData)
            }
        },

        //紧急预案接口
        getUrgencyTaskList:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/urgencyList", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/urgencyList", postData)
            }
        },
        getUrgencyTaskById:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/getUrgencyTaskById", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/getUrgencyTaskById", postData)
            }
        },
        setUrgencyTerminal:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/setReadyOrFailedTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/setReadyOrFailedTerminal", postData)
            }
        },
        urgencyAddOrUpdate:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/addOrUpdate", postData)
            }
        },
        delUrgencyTask:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/delTask", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/delTask", postData)
            }
        },

        stopUrgencyTask:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/stopUrgencyTask", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/stopUrgencyTask", postData)
            }

        },

        startUrgencyTask:function(postData, callback){
            if(callback){
                this.httpPost("/urgency/startUrgencyTask", postData, callback)
            }else {
                return this.syncHttpPost("/urgency/startUrgencyTask", postData)
            }
        },


        //自动开关机设置接口
        getBootSettingList:function (postData, callback) {
            if(callback){
                this.httpPost("/bootsetting/getsettings", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/getsettings", postData)
            }
        },
        getBootSettingById:function(postData, callback){
            if(callback){
                this.httpPost("/bootsetting/getsettingById", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/getsettingById", postData)
            }
        },

        setBootSetTerminal:function(postData, callback){
            if(callback){
                this.httpPost("/bootsetting/setReadyOrFailedTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/setReadyOrFailedTerminal", postData)
            }
        },
        clearBootSetting:function(postData, callback){
            if(callback){
                this.httpPost("/bootsetting/clearBootSetting", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/clearBootSetting", postData)
            }
        },


        bootSetAddOrUpdate:function (postData, callback) {
            if(callback){
                this.httpPost("/bootsetting/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/addOrUpdate", postData)
            }
        },
        sendBootSet2Terminal:function (postData, callback) {
            if(callback){
                this.httpPost("/bootsetting/sendToTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/sendToTerminal", postData)
            }
        },
        delBootSetting:function (postData, callback) {
            if(callback){
                this.httpPost("/bootsetting/del_setting", postData, callback)
            }else {
                return this.syncHttpPost("/bootsetting/del_setting", postData)
            }
        },

        //自动开关灯设置接口

        getLampSettingList:function (postData, callback) {
            if(callback){
                this.httpPost("/lampsetting/getsettings", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/getsettings", postData)
            }
        },
        getLampsettingById:function(postData, callback){
            if(callback){
                this.httpPost("/lampsetting/getsettingById", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/getsettingById", postData)
            }
        },

        setLampTerminal:function(postData, callback){
            if(callback){
                this.httpPost("/lampsetting/setReadyOrFailedTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/setReadyOrFailedTerminal", postData)
            }
        },
        lampSetAddOrUpdate:function (postData, callback) {
            if(callback){
                this.httpPost("/lampsetting/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/addOrUpdate", postData)
            }
        },
        sendLampSet2Terminal:function (postData, callback) {
            if(callback){
                this.httpPost("/lampsetting/sendToTerminal", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/sendToTerminal", postData)
            }
        },
        clearLampSetting:function(postData, callback){
            if(callback){
                this.httpPost("/lampsetting/clearLampSetting", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/clearLampSetting", postData)
            }
        },
        delLampSetting:function (postData, callback) {
            if(callback){
                this.httpPost("/lampsetting/del_setting", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/del_setting", postData)
            }
        },
        setLampOnOrOff:function(postData, callback){
            if(callback){
                this.httpPost("/lampsetting/setLampOnOrOff", postData, callback)
            }else {
                return this.syncHttpPost("/lampsetting/setLampOnOrOff", postData)
            }
        },
        //推流接口
        getLivePlayList:function (postData, callback) {
            if(callback){
                this.httpPost("/liveplay/list", postData, callback)
            }else {
                return this.syncHttpPost("/liveplay/list", postData)
            }
        },
        addOrUpdateLivePlay:function (postData, callback) {
            if(callback){
                this.httpPost("/liveplay/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/liveplay/addOrUpdate", postData)
            }
        },
        deleteLivePlay:function (postData, callback) {
            if(callback){
                this.httpPost("/liveplay/delete", postData, callback)
            }else {
                return this.syncHttpPost("/liveplay/delete", postData)
            }
        },

        liveplayStarStop:function(postData, callback){
            if(callback){
                this.httpPost("/liveplay/startOrStop", postData, callback)
            }else {
                return this.syncHttpPost("/liveplay/startOrStop", postData)
            }
        },
        GetPlayinglist:function(postData, callback){
            if(callback){
                this.httpPost("/liveplay/playinglist", postData, callback)
            }else {
                return this.syncHttpPost("/liveplay/playinglist", postData)
            }
        },
        //第三方服务器操作接口
        getExtendserverList:function (postData, callback) {
            if(callback){
                this.httpPost("/extendserver/list", postData, callback)
            }else {
                return this.syncHttpPost("/extendserver/list", postData)
            }
        },
        addOrUpdateServerInfo:function (postData, callback) {
            if(callback){
                this.httpPost("/extendserver/addOrUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/extendserver/addOrUpdate", postData)
            }
        },
        deleteServerInfo:function (postData, callback) {
            if(callback){
                this.httpPost("/extendserver/delete", postData, callback)
            }else {
                return this.syncHttpPost("/extendserver/delete", postData)
            }
        },

        //服务器升级接口
        getServerUpdateInfo:function (postData, callback) { //获取升级信息
            if(callback){
                this.httpPost("/system/getServerUpdateInfo", postData, callback)
            }else {
                return this.syncHttpPost("/system/getServerUpdateInfo", postData)
            }
        },

        setServerUpdate:function (postData, callback) {//执行升级或者重启tomcat操作
            if(callback){
                this.httpPost("/system/setServerUpdate", postData, callback)
            }else {
                return this.syncHttpPost("/system/setServerUpdate", postData)
            }
        },
        getServerUpateHistory:function (postData, callback) {
            if(callback){
                this.httpPost("/system/getServerUpateHistory", postData, callback)
            }else {
                return this.syncHttpPost("/system/getServerUpateHistory", postData)
            }
        },

        //用户分组接口
        getUserGroupList:function (postData, callback) {
            if(callback){
                this.httpPost("/usergroup/getUserGroupList", postData, callback)
            }else {
                return this.syncHttpPost("/usergroup/getUserGroupList", postData)
            }
        },
        addOrUpdateUserGroup:function (postData, callback) {
            if(callback){
                this.httpPost("/usergroup/addOrUpdateUserGroup", postData, callback)
            }else {
                return this.syncHttpPost("/usergroup/addOrUpdateUserGroup", postData)
            }
        },
        delUserGroups:function(postData, callback){
            if(callback){
                this.httpPost("/usergroup/delUserGroups", postData, callback)
            }else {
                return this.syncHttpPost("/usergroup/delUserGroups", postData)
            }
        },
        addOrRemoveUser:function (postData, callback) {
            if(callback){
                this.httpPost("/usergroup/addOrRemoveUser", postData, callback)
            }else {
                return this.syncHttpPost("/usergroup/addOrRemoveUser", postData)
            }
        },
        addOrRemoveTerminalGrp:function (postData, callback) {
            if(callback){
                this.httpPost("/usergroup/addOrRemoveTerminalGrp", postData, callback)
            }else {
                return this.syncHttpPost("/usergroup/addOrRemoveTerminalGrp", postData)
            }
        },



    }

    //输出模块
    exports('server_api', obj);
});