layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});

var fileTypeMap = {
    "image": 1,
    "audio": 2,
    "video": 3,
    "zip": 4, //升级文件
    "unknow":100
}

var fileInputAcept = {
    "image": "image/png,image/jpeg,image/jpg",
    "audio": "audio/mp3,audio/wave,audio/wma,audio/aac",
    "video": "video/mp4,video/flv,video/mkv,video/mpeg,video/avi,video/H264",
    "zip":"application/zip",
    "unknow":"all"
    // "apk": "application/vnd.android.package-archive"

}

var fileSupport = {
    "image": [".jpg",".jpeg",".png"],
    "audio": [".mp3",".wave",".wma",".aac"],
    "video": [".mp4",".flv",".mkv",".rmvb",".avi",".mpeg"],
    "zip": [".zip"], //升级文件
}

var fileSupportShow ={
    "image": "仅支持:jpg,jpeg,png",
    "audio": "仅支持:mp3,wave,wma,aac",
    "video": "仅支持:mp4,flv,mkv,rmvb,avi,mpeg",
    "zip": "仅支持zip压缩格式", //升级文件
    "unknow":"其他类型不可上传,仅支持查看"
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var fileType = "all";

layui.use(['jquery', 'server_api', 'laypage', 'table', 'element'], function () {
    var $ = layui.jquery;
    var upload = layui.upload;
    var server_api = layui.server_api;
    var laypage = layui.laypage;
    var table = layui.table;
    var element = layui.element;


    function popWindow(title, elem, size) {
        var index = layer.open({
            type: 1,
            shade: 0.1,
            shadeClose: true,
            title: title, //不显示标题
            closeBtn: 1,
            resize: false,
            area: size,//['500px', '300px'],
            content: $(elem), //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
            yes: function (index, layero) {
                //do something
            },
            cancel: function () {

            }
        });
        return index;
    }

    function delAttachFiles(attach_ids){
        var postData = {
            attach_ids: attach_ids
        }
        server_api.delFiles(JSON.stringify(postData), function (resp) {
            setTimeout(function () {
                if (resp.status == 0) {
                    layer.msg("删除文件成功!", {icon: 1})
                    addOrDelItem("del", attach_ids.length)
                    setCurrentData("yes")
                } else {
                    layer.msg(resp.msg, {icon: 2})
                }
            },10)
        })
    }

    table.on('tool(attachmentTable)', function (obj) {
            var data = obj.data;
            var event = obj.event;
            if (event == "delThisFile") {
                layer.confirm('是否真的删除此文件?', {icon: 3, title:'提示'}, function(index){
                    var attach_ids = [data.attach_id]
                    delAttachFiles(attach_ids)
                })
            }else if(event == "previewFile"){
                if(data.attach_type >3){
                    layer.msg("该类型文件不可以预览",{icon:0})
                    return
                }

                var title =""
                var mediaSrc =""
                if(data.attach_type==1){
                    title = "图片[" + data.name  + "]预览"
                    mediaSrc =  server_api.getDownLoadUrl(data.attach_id)
                    $("#imagePreview").find("img").attr("src",mediaSrc)

                    popWindow(title,"#imagePreview",["60%","60%"])
                }else if(data.attach_type==2){
                    title = "音频[" + data.name  + "]预览"
                    mediaSrc =  server_api.getDownLoadUrl(data.attach_id)
                    $("#audioPreview").find("audio").attr("src",mediaSrc)

                    popWindow(title,"#audioPreview",["60%","140px"])
                }else if(data.attach_type==3){
                    title = "视频[" + data.name  + "]预览"
                    mediaSrc =  server_api.getDownLoadUrl(data.attach_id)
                    $("#videoPreview").find("video").attr("src",mediaSrc)
                    $("#videoPreview").find("video").css("width","100%")
                    $("#videoPreview").find("video").css("height","100%")

                    popWindow(title,"#videoPreview",["60%","60%"])
                }
            }
        }
    );

    //头工具栏事件
    table.on('toolbar(attachmentTable)', function (obj) {
        var checkStatus = table.checkStatus(obj.config.id);
        switch (obj.event) {
            case 'batchDelFile':
                if(checkStatus.data.length==0){
                    layer.msg("未选中任何文件!",{icon:2})
                    return
                }else{
                    var attach_ids = []
                    var data = checkStatus.data;
                    //layer.alert(JSON.stringify(data));
                    for (var i = 0; i < data.length; i++) {
                        attach_ids.push(data[i].attach_id)
                    }
                    layer.confirm('是否真的删除勾选的文件?', {icon: 3, title:'提示'}, function(index){
                        delAttachFiles(attach_ids)
                    })
                }
                break;
        };
    });

    table.on('edit(attachmentTable)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段
        var data = obj.data;
        var postData = data;
        if (field == "name") {
            var postData = {
                attach_id: data.attach_id,
                new_name: value
            }
            var newdata = {};
            if(value==null ||value.length==0){
                newdata[field] = data[field];
                obj.update(newdata);
                layer.msg("不能修改为空!",{icon:2})
                setCurrentData()
            }else{
                server_api.modifyFileName(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        newdata[field] = value;
                        obj.update(newdata);
                        layer.msg("修改文件名成功!", {icon: 1})
                    } else {
                        newdata[field] = data[field];
                        obj.update(newdata);
                        layer.msg("修改文件名失败!", {icon: 2})
                        setCurrentData()
                    }
                });
            }
        }

    });


    function addOrDelItem(action, number) {
        if (action == "add") {
            pageData.total = pageData.total + number;
        } else if (action == "del") {
            pageData.total = pageData.total - number;
        }

        var m = pageData.total / pageData.pagesize;
        var n = pageData.total % pageData.pagesize;
        pageData.lastPage = n > 0 ? m + 1 : n;
    }

    function renderPages(total) {
        laypage.render({
            elem: 'pages'
            , count: total
            , layout: ['count', 'prev', 'page', 'next', 'limit', 'refresh', 'skip']
            , jump: function (obj) {
                pageData.total = total;
                pageData.page = obj.curr;
                pageData.pagesize = obj.limit;
                var m = total / pageData.pagesize;
                var n = total % pageData.pagesize;
                pageData.lastPage = n > 0 ? m + 1 : n;

                if (pageData.total > 0) {
                    setCurrentData();
                }
            }
        });
    }


    function renderDataList(dataList) {

        var renderOpt ={
            id: "attachmentTable",//
            elem: '#attachmentTable',//指定表格元素
            data: dataList,  //表格当前页面数据
            limit: pageData.pagesize,
            defaultToolbar: ['filter'],
            skin: 'line', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
                $("#showSupport").text(fileSupportShow[fileType])
                $("#selectFile").find('[name=fileinfo]').attr("accept", fileInputAcept[fileType])

                function doUpload(fileList,cur,total){
                    if(cur >=total){
                        return
                    }

                    var isSupport = false
                    var fileInfo = fileList[cur];
                    var supportList = fileSupport[fileType]
                    if(!supportList){
                        layer.msg("不支持上传的文件类型!",{icon:0})
                        return
                    }else{
                        var tmpFileName = fileInfo.name.toLowerCase()
                        for(var i=0;i<supportList.length;i++){
                            if (tmpFileName.lastIndexOf(supportList[i])!=-1){
                                isSupport = true
                                break
                            }
                        }
                    }

                    if(isSupport==true){
                        var showMsg = "正在上传《"+fileInfo.name + "》，请稍候"
                        var loadingFlag = layer.msg(showMsg, {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                        $("#progressDiv").show();
                        server_api.httpUploadFile(fileInfo, function (resp) {
                            //loaded代表上传了多少
                            //total代表总数为多少
                            var tmpRate = (resp.loaded / resp.total) * 100
                            if(tmpRate > 95){
                                tmpRate =99
                            }
                            var progressRate = Math.floor(tmpRate) //tmpRate.toFixed(2) * 100;

                            element.progress('uploadProgress', progressRate + "%");
                            fileUploading = true
                            if (resp.loaded == resp.total && resp.loaded > 0) {
                                var waitTime = 2*1000

                                if(resp.total > 100*1024*1024){
                                    waitTime = 15*1000
                                }else if(resp.total > 30*1024*1024){
                                    waitTime = 5*1000
                                }

                                setTimeout(function () {
                                    $("#progressDiv").hide()
                                    layer.close(loadingFlag)
                                    addOrDelItem("add",1)
                                    setCurrentData("yes")
                                    element.progress('uploadProgress', 100 + "%");
                                    doUpload(fileList,cur+1,total)
                                }, waitTime)
                            }
                        })
                    }else{
                        layer.msg("不支持的文件类型!",{icon:0})
                        return
                    }
                }
               // console.log("fileInputAcept[fileType]==",fileInputAcept[fileType])
                $("#fileUpload").change(function () {
                    var fileList = $("#fileUpload")[0].files
                    var fileCnt = fileList.length
                    doUpload(fileList,0,fileCnt)
                });
            },
            cols: [[
                {checkbox: true},
                {field: 'id', title: '序号', width: 80, sort: true},
                {title: '操作', toolbar: '#itemAction', width: 160},
                {field: 'attach_id', title: '附件ID', width: 140},
                {field: 'upload_user', title: '上传者', width: 120, sort: true},
                {field: 'name', title: '附件名字(点击可修改)', width: 300, sort: true, edit: "text"},
                {field: 'save_path', title: '附件存储路径(红色表示文件不存在)', width: 500, sort: true,templet:function (val) {
                        if(val.exist==0){
                            return "<div class='color-red'>"+ val.save_path +"</div>"
                        }else{
                            return "<div>"+ val.save_path +"</div>"
                        }
                    }},
                {
                    field: 'attach_type', title: '附件类型', width: 120, sort: true, templet: function (val) {
                        for (var key in fileTypeMap) {
                            if (fileTypeMap[key] == parseInt(val.attach_type)) {
                                return key;
                            }
                        }
                    }
                },
                {
                    field: 'size', title: '附件大小', width: 140, sort: true, templet: function (val) {
                        var sizeInt = parseInt(val.size);
                        if (sizeInt / 1024 > 1024) {
                            var tmpInt = sizeInt / 1024
                            var msize = tmpInt /1024
                            return msize.toFixed(2) + "M"
                        } else if(sizeInt > 1024) {
                            var ksize = sizeInt / 1024
                            return ksize.toFixed(2) + "KB"
                        }else{
                            return sizeInt.toFixed(2) + "B"
                        }
                    }
                },
                {field: 'create_time', title: '上传时间', width: 200, templet: function (val) {
                    return val.create_time.substring(0,19)
                  }
                }
            ]]
        }

        if(fileType!="unknow"){
            renderOpt.toolbar='#toolbarAction'
        }

        table.render(renderOpt);

    }


    function setCurrentData(getTotal, isLast, isFirs) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            file_type: fileTypeMap[fileType],
            getTotal: getTotal
        }

        // if(isLast && isLast =="yes"){
        //     postData.page =  pageData.lastPage;
        // }else if(isFirst && isFirst=="yes"){
        //     postData.page = 1;
        // }

        server_api.getFileList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                dataList = resp.result;
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                }
            } else {
                renderPages(0);
                layer.msg(resp.msg, {icon: 2});
            }
            renderDataList(dataList);
        });
    }

    function GetQueryString(name) {
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
        var r = window.location.search.substr(1).match(reg); //获取url中"?"符后的字符串并正则匹配
        var context = "";
        if (r != null)
            context = r[2];
        reg = null;
        r = null;
        return context == null || context == "" || context == "undefined" ? "" : context;
    }


    $(function () {

        fileType = GetQueryString("filetype");

        setCurrentData("yes");

    })


});