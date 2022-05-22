layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});


var terminalState = {
    0: "离线",
    1: "空闲",
    2: "任务中",
    3: "播流中",
    4: "未知",
    5:"推流中",
    6:"推拉流中"
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;

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

    function setCurrentData(getTotal, helpStatus, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            getTotal: getTotal
        }

        if (helpStatus && helpStatus.length > 0) {
            postData.help_status = helpStatus
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getTerminalHelpList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                //console.log(resp.result)
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


    function parseDataList(dataList) {
        var restDataList = []
        for (var i = 0; i < dataList.length; i++) {
            var taskInfo = JSON.parse(dataList[i].content)
            var newDataItem = {}
            newDataItem.id = dataList[i].id
            newDataItem.task_name = taskInfo.taskName
            newDataItem.task_status = function (start_date, end_date, result) {
                var starttime = new Date(Date.parse(start_date));
                var endtime = new Date(Date.parse(end_date));
                if (result == "terminal_del") {
                    return "终端已清除"
                }
                if (starttime > new Date()) {
                    return "未开始"
                } else if (endtime < new Date()) {
                    return "已经结束"
                } else {
                    return "执行中"
                }
            }(taskInfo.from, taskInfo.to, dataList[i].result)
            newDataItem.create_time = dataList[i].create_time
            newDataItem.task_type = taskTypeMap[taskInfo.taskType]
            newDataItem.task_set = function (theTask) {
                var showSrt = ""
                showSrt += "优先级:" + priorityMap[theTask.level] + "<br>"
                showSrt += "循环方式:" + playModeMap[theTask.playDate.mode] + "<br>"
                showSrt += "开始日期:" + taskInfo.playDate.from + ",结束日期:" + taskInfo.playDate.to + "<br>"
                return showSrt
            }(taskInfo)

            restDataList.push(newDataItem)
        }

        return restDataList

    }


    function renderDataList(dataList) {
        table.render({
            id: "terminalHelpList",//
            elem: '#terminalHelpList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
                // for(var i =0;i<res.data.length;i++){
                //     if(res.data[i].task_name =="新的任务名字"){
                //         $("tr").eq(i+1).css("background-color","rgba(255,88,39,0.4)").css("color","white")
                //     }
                // }
                $(".layui-table-main  tr").each(function (index, val) {
                    $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                });
            },
            // parseData: function (res) { //res 即为原始返回的数据
            //     console.log("!!!!!!!!!!!!!!!!!!!run into rable parse data");
            //     console.log(res)
            //     return res
            // },
            cols: [[
                {checkbox: true},
                {field: 'id', align: 'center', title: '序号', width: 80, sort: true},
                // {field: 'help_id', align: 'left', title: '求助编号', width: 260, sort: true},
                {field: 'terminal_id', align: 'center', title: '终端编号', width: 240, sort: true},
                {field: 'terminalIP', align: 'center', title: '终端ip', width: 140, sort: true},

                {field: 'terminalState', align: 'center', title: '终端状态', width: 120, sort: true,templet:function (val) {
                        if(val.terminalState==0){
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + terminalState[val.terminalState] + '</span>';
                        }else{
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + terminalState[val.terminalState] + '</span>';
                        }
                    }},
                {field: 'terminalAddr', align: 'left', title: '安装地址', width: 200},
                {field: 'help_time', align: 'center', title: '求助时间', width: 160, sort: true},
                {
                    field: 'help_status',
                    align: 'center',
                    title: '求助状态',
                    width: 140,
                    event: "help_status",
                    sort: true,
                    templet: function (val) {
                        if (val.help_status == 0) {
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">未处理</span>';
                        } else {
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">已处理</span>';
                        }

                    }
                },
                {
                    field: 'sent_task',
                    align: 'center',
                    title: '下发任务',
                    width: 140,
                    event: "sent_task",
                    sort: true,
                    templet: function (val) {
                        return '<span class="layui-btn layui-btn-xs">点击下发或停止任务</span>';
                    }
                },

                {
                    field: 'video_url',
                    align: 'center',
                    title: '视频记录',
                    width: 140,
                    event: "ViewRecordVideo",
                    sort: true,
                    templet: function (val) {
                        return '<span class="layui-btn layui-btn-xs">查看录像</span>';

                    }
                },
                {title: '操作', align: 'center', toolbar: '#helpActions'}


            ]]
        })
    }

    //监听任务表单项修改
    table.on('tool(terminalHelpList)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if (event == "help_status") {
            if (data.help_status == 0) {
                layer.confirm("是否调整状态为已处理?", {btn: ['确定', '取消'], title: "提示", icon: 3}, function (index) {
                    var postData = {
                        help_id: data.help_id,
                        help_status: 1,
                    }

                    server_api.modifyHelpSatus(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("调整状态成功!", {icon: 1})
                            var updateItem = {}
                            updateItem.help_status = 1
                            obj.update(updateItem)

                        } else {
                            layer.msg("调整状态失败!", {icon: 2})
                        }
                    })
                    layer.close(index)
                })
            } else {
                layer.msg("求助已经处理过了!", {icon: 0})
            }
        } else if (event == "view-video") {
            if (data.terminalState == 0) {
                layer.msg("终端不在线！", {icon: 2})
                return
            }
            var postData = {
                terminal_id: data.terminal_id,
                need_agency: 1,
                action: "start"
            }
            server_api.getTerminalVideo(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    var title = "调取终端(" + data.terminalIP + ")" + "视频" + ",终端安装地址:" + data.terminalAddr
                    var videoUrl = resp.result[0]["pull_url"]
                    var stream_session = resp.result[0]["stream_session"]
                    var params = "videoUrl=" + videoUrl + "&type=rtmp/flv" + "&stream_session=" + stream_session
                    WeAdminShow(title, '../video/play.html?' + params);
                } else {
                    layer.msg("调取视频失败!", {icon: 2})
                }
            })
        } else if (event == "ViewRecordVideo") {

            var postData = {
                help_id: data.help_id,
                page: 1,
                pagesize: 10000
            }


            server_api.getHelpRecordVideoList(JSON.stringify(postData), function (resp) {
                var treeDataList = []
                var allVideos = {}
                allVideos.title = "录像视频文件列表"
                allVideos.id = 1
                allVideos.children = []
                allVideos.spread = true
                treeDataList.push(allVideos)
                if (resp.status == 0) {
                    var videoList = resp.result
                    if (videoList.length > 0) {
                        for (var i = 0; i < videoList.length; i++) {
                            var oneVideo = {}
                            oneVideo.title = videoList[i].name
                            var sizeInt = videoList[i].size
                            if (sizeInt / 1024 > 1024) {
                                var msize = sizeInt / 1024 / 1204
                                oneVideo.title += " (" + msize.toFixed(2) + "M" + ")"
                            } else if (sizeInt > 1024) {
                                var ksize = sizeInt / 1024
                                oneVideo.title += " (" + ksize.toFixed(2) + "KB" + ")"
                            } else {
                                oneVideo.title += " (" + sizeInt.toFixed(2) + "B" + ")"
                            }
                            oneVideo.id = i + 1

                            allVideos.children.push(oneVideo)
                        }
                    }

                    tree.render({
                        id: 'videoListTree'
                        , elem: '#videoListTree'
                        , data: treeDataList
                        , click: function (obj) {
							var thisData = obj.data
							var name_list =[obj.data.title.split("(")[0].trim()]

							var postData ={
								help_id:data.help_id,
								name_list:name_list
							}

                            var loadingFlag = layer.msg('正在上传，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});

							var refreshPlayUrl = function(){

                                server_api.uploadHelpVideo(JSON.stringify(postData),function (resp) {
                                    if(resp.status==0){
                                        var successVideos = resp.result
                                        if(successVideos && successVideos.length ==1){
                                            var videoURl = server_api.getPlayHelpVideUrl(data.help_id,successVideos[0])
                                            $("#videoList").find("video").attr("src",videoURl)
                                            $("#videoList").find("[id='currentPlay']").text("当前播放的视频是:"+successVideos[0])
                                            layer.close(loadingFlag)
                                            layer.msg("获取视频成功!请点击播放按钮。",{icon:1})
                                        }else{
                                            setTimeout(refreshPlayUrl,5000)
                                        }
                                    }else{
                                        layer.msg("获取录像视频失败!",{icon:2})
                                    }
                                })
                            }
                            refreshPlayUrl()
                        }
                    });

                }else{
                    layer.msg("获取录像视频列表失败!",{icon:2})
                    tree.render({
                        id: 'videoListTree'
                        , elem: '#videoListTree'
                        , data: treeDataList
                        , click: function (obj) {
                        }
                    });

                }
            })


            popWindow("查看求助录像视频", "#videoList", ["90%", "80%"])

        }else if(event=="sent_task"){
            layer.open({
                type: 2,
                shade: 0.5,
                shadeClose: true,
                title: "下发或者停止任务", //不显示标题
                closeBtn: 1,
                resize: false,
                area: ['100%', '60%'],
                content: "../task/tasklist.html?plan_type=temporary", //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
                yes: function (index, layero) {
                    //do something
                    console.log("run into yes!")
                },
                cancel: function () {

                }
            });
        }

    })

    function popWindow(title, elem, size) {
        var index = layer.open({
            type: 1,
            shade: 0.5,
            shadeClose: true,
            title: title, //不显示标题
            closeBtn: 1,
            resize: false,
            area: size,//['500px', '300px'],
            content: $(elem), //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
            yes: function (index, layero) {
                //do something
                console.log("run into yes!")
            },
            cancel: function () {

            }
        });
        return index;
    }


    $(function () {
        setCurrentData("yes", "all")

    })


})