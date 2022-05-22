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

var taskTypeMap = {
    1: "文本类型",
    2: "图片类型",
    3: "音频类型",
    4: "视频类型",
}

var taskTableMap = {
    "TaskInfo":"task_id",
    "BootSetting":"setting_id",
    "LampSetting":"setting_id",
    "ShieldTask":"shield_id",
    "UrgencyTask":"urgency_id"
}



var terminalTypeList={
    "normal":"大屏终端",
    "loudspeaker":"外接音箱",
    "led":"外接LED大屏"
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var tableIns = null
var search_key =""
var currentUser = null


layui.use(['jquery', 'form','tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;

    //全局变量区域
    var groupList = null;
    var currentGrp = "all-group";
    var currentPopIndex = null;
    form.render()

    showViewVedio = function(title, url, area,callback) {
        if(title == null || title == '') {
            title = false;
        };
        if(url == null || url == '') {
            url = "404.html";
        };
        layer.open({
            type: 2,
            area: area,
            fix: false, //不固定
            maxmin: true,
            shadeClose: true,
            shade: 0.4,
            title: title,
            content: url,
            end: function() {
                if(typeof(callback)=="function"){
                    callback()
                }
            }
        });
    }


    function popWindow(title, elem, size,url,callback) {
        var theContent = elem?$(elem):url
        var index = layer.open({
            type: 1,
            shade: 0.5,
            shadeClose: true,
            title: title, //不显示标题
            closeBtn: 1,
            resize: false,
            area: size,//['500px', '300px'],
            content: theContent, //捕获的元素，注意：最好该指定的元素要存放在body最外层，否则可能被其它的相对元素所影响
            yes: function (index, layero) {
                //do something("run into yes!")
            },
            cancel: function () {


            },
            end : function() {
                if(typeof(callback)=="function"){
                    callback()
                }
            }

        });
        return index;
    }


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
            ,limits:[10,20,50,100,200,500,10000]
            , layout: ['count', 'prev', 'page', 'next', 'limit', 'refresh', 'skip']
            , jump: function (obj) {
                pageData.total = total;
                pageData.page = obj.curr;
                pageData.pagesize = obj.limit;
                var m = total / pageData.pagesize;
                var n = total % pageData.pagesize;
                pageData.lastPage = n > 0 ? m + 1 : n;

                if (total > 0) {
                    setCurrentData();
                }
            }
        });
    }


    function renderDataList(dataList) {
        var renderOpt = {
            id: "terminalList",//
            elem: '#terminalList',//指定表格元素
            data: dataList,  //表格当前页面数据
            toolbar: "#toolbarAction",
            defaultToolbar: ['filter'],
            limit: pageData.pagesize,
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count){
                // for(var i =0;i<res.data.length;i++){
                //     $("tr").eq(i+1).find(".volumVal").text("当前:" + res.data[i].volume + " ")
                // }

                $("input[name='search']").off("keypress");
                $("input[name='search']").on('keypress', function (event) {
                    if (event.keyCode == "13") {
                        //需要处理的事情
                        search_key = $("input[name='search']").val()
                        setCurrentData("yes")
                    }
                });

                $("input[name='search']").val(search_key)
            },

            cols: [[
                {checkbox: true},
                {field: 'id', title: '序号',align:'center',width: 80},
                {title: '操作',align:'center', toolbar: '#cellAction', width: 320},
                // {field: 'terminal_id', title: '终端ID', width: 240, sort: true},
                {
                    field: 'state',align:'center', title: '设备状态', width: 120, sort: true, templet: function (val) {
                        if(val.state==0){
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + terminalState[val.state] + '</span>';
                        }else{
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + terminalState[val.state] + '</span>';
                        }
                    }
                },
                {field: 'volume',align:'center', title: '终端音量', width: 180, sort: true,templet:function (val) {
                        var changePart = '<a class="volumVal" style="width:40%;">当前:' +val.volume + ' </a>\n'
                        return '<div class="align-space">\n' + changePart +
                            '<a class="layui-btn  layui-btn-xs" lay-event="incr" style="width:20%;"><i class="fa fa-plus icon-margin"\n' +
                            'style="font-size:14px;"></i></a>\n' +
                            '<a class="layui-btn  layui-btn-xs" lay-event="dec" style="width:20%;"><i class="fa fa-minus icon-margin"\n' +
                            'style="font-size:14px;"></i></a>\n' +
                            '</div>'
                    }},

                {field: 'ip',align:'center', title: '终端IP', width: 140, sort: true},
                {field:'terminal_type',title: '设备类型',align:'center',width:120, sort: true,templet:function (val) {

                        if(val.terminal_id.substring(3,5).indexOf("02")!=-1){
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">广播终端</span>';
                        }else if(val.terminal_id.substring(3,5).indexOf("03")!=-1){
                            return '<span class="layui-btn layui-btn-xs layui-btn-normal">音箱终端</span>';
                        }else if(val.terminal_id.substring(3,5).indexOf("04")!=-1){
                            return '<span class="layui-btn layui-btn-xs layui-btn-container">LED大屏</span>';
                        }else{
                            return '<span class="layui-btn layui-btn-xs layui-btn-primary">普通终端</span>';
                        }
                    }},
                {field: 'name', title: '终端名称(可编辑)',align:'center', width: 160, sort: true,edit: 'text'},
                {field: 'install_addr', title: '安装地址(可编辑)',align:'center', width: 200, sort: true, edit: 'text'},
                {
                    field: 'gids', title: '是否分组',align:'center', width: 120, sort: true, templet: function (val) {
                        if (val.gids && val.gids.length > 0) {
                            return "是";
                        } else {
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">否</span>';
                        }
                    }
                },
                {field: 'boot_time', title: '开机时间',align:'center', width: 120, sort: true},
                {field: 'shutdown_time', title: '关机时间', align:'center',width: 120, sort: true},
                {field: 'app_ver', title: '终端APP版本',align:'center', width: 200, sort: true},
                {
                    field: 'err_msg', title: '异常信息',align:'center', width: 260, sort: true, event: 'err_msg', templet: function (val) {
                        if (!val.err_msg || val.err_msg.length == 0) {
                            return "无"
                        } else {
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">' + val.err_msg + '</span>';
                        }

                    }
                }

            ]]
        }

        if(!tableIns){
            tableIns = table.render(renderOpt);
        }else{
            tableIns.reload(renderOpt);
        }
    }


    function setCurrentData(getTotal,isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            search_key:search_key
        }

        if(getTotal && getTotal.length >0){
            postData.getTotal = getTotal;
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        if (currentGrp && currentGrp.length > 0) {
            postData.gid = currentGrp;
        }


        //renderDataList([])
        server_api.getTerminalList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                //console.log(resp.result)
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                    //renderPages(1000);
                }
            } else {
                renderPages(0);
                layer.msg(resp.msg, {icon: 2});
            }
            renderDataList(dataList);
        });
    }


    form.on('select(groupSelect)', function (data) {

        if (data.value && data.value == "all-group") {
            currentGrp = "all-group";
        } else {
            currentGrp = data.value;
        }
        pageData.page =1
        setCurrentData("yes");
    });

    $("#searchTerminal").on("click",function (object) {
        server_api.findTerminals(null,function (resp) {
            var timeOutIndex = setTimeout(function () {
                layer.msg("网络可能存在问题!",{icon:0})
            },3000)
            if(resp.status==0){
                clearTimeout(timeOutIndex)
                layer.msg("发现指令已经发送,请刷新设备列表!",{icon:1})
                setCurrentData("yes")
            }
        })
    })


    //监听提交
    form.on('submit(grpChangeSubmit)', function (data) {
        var toGrp = data.field.gid.split(":")[1];
        var terminalData = null;
        var action = data.field.gid.split(":")[0];
        var terminal_ids = [];
        terminalData = localStorage.getItem(action + ":terminals");

        if (terminalData && terminalData.length > 0) {
            var terminalDataJson = JSON.parse(terminalData);
            for (var i = 0; i < terminalDataJson.length; i++) {
                var terminalItem = terminalDataJson[i];
                // if(terminalItem.state==0){
                //     layer.msg("请勿选择未在线的终端!",{icon:2})
                //     return false;
                // }
                terminal_ids.push(terminalItem.terminal_id);
                if(terminalItem.gids.length >0 && currentGrp=="all-group"){
                    layer.msg("终端:" + terminalItem.ip + "　已经分组,请不要在所有分组下操作!",{icon:2})
                    return false
                }
            }

            var postData = {
                change_type: action,
                terminal_ids: terminal_ids,
                from_gid: currentGrp,
                to_gid: toGrp
            }

            server_api.changeGroup(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("变更终端分组成功!", {icon: 1});
                    if (currentPopIndex && currentPopIndex > 0) {
                        layer.close(currentPopIndex);
                    }
                    setCurrentData("yes")
                } else {
                    layer.msg("变更终端分组失败!", {icon: 2});
                }
            });
        } else {
            layer.msg("不存在需要变更分组的终端!", {icon: 2})
        }
        return false;
    });



    //监听工具栏事件

    table.on('toolbar(terminalList)', function (obj) {
        var event = obj.event
        //var data = obj.data
        var data = table.checkStatus('terminalList').data;
        if(data.length<=0 && event!="LAYTABLE_COLS" && event !="doSearch"){
            layer.msg("未勾选任何终端!",{icon:0})
            return
        }
        if (event == "delSelectTerminal") {
            //批量删除
            if (data && data.length > 0) {
                layer.confirm('删除勾选的终端，如果终端再次上线会自动增加!', function (index) {
                    var terminal_ids = [];
                    for (var i = 0; i < data.length; i++) {
                        terminal_ids.push(data[i].terminal_id);
                        if (data[i].state >= 1) {
                            layer.msg("不可删除在线的终端!", {icon: 2})
                            return
                        }
                    }
                    var postData = {terminal_ids: terminal_ids};
                    server_api.delTerminal(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            layer.msg('删除成功', {
                                icon: 1
                            });
                            addOrDelItem("del", terminal_ids.length);
                            setCurrentData("yes");
                        } else {
                            layer.msg('删除失败', {
                                icon: 2
                            });
                        }
                    });
                });
            } else {
                layer.msg('请选择需要删除的终端', {
                    icon: 0
                });
            }
        } else if (event == "move2Group") {
            popChangeGroupWin("move");
        } else if (event == "copy2Group") {
            popChangeGroupWin("copy");
        } else if (event == "incrVolume") {
            doChangeVolume("incr",obj)
        } else if (event == "decVolume") {
            doChangeVolume("dec",obj)
        } else if (event == "cleanTerminalTask") {
            cleanTerminalTask()
        } else if (event == "delTerminalTaskFile") {
            var terminal_ids = [];
            for (var i = 0; i < data.length; i++) {
                terminal_ids.push(data[i].terminal_id);
                if (data[i].state == 0) {
                    layer.msg("不可勾选不在线的终端!", {icon: 2})
                    return
                }
            }

            var postData = {
                clean_type: "task_file",
                terminal_ids: terminal_ids
            }

            server_api.cleanTask(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("删除终端任务文件成功!", {icon: 1})
                } else {
                    layer.msg("删除终端任务文件失败!", {icon: 1})
                }
            })

        }else if(event=="delCurrentTask"){
            var terminal_ids = [];
            for (var i = 0; i < data.length; i++) {
                terminal_ids.push(data[i].terminal_id);
                if (data[i].state == 0) {
                    layer.msg("不可勾选不在线的终端!", {icon: 2})
                    return
                }
            }
            var postData = {
                clean_type: "current_task",
                terminal_ids: terminal_ids
            }

            server_api.cleanTask(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("删除终端当前任务成功!", {icon: 1})
                } else {
                    layer.msg("删除终端当前任务失败!", {icon: 2})
                }
            })
        }else if(event=="rebootNow"){
            var terminal_ids = [];
            for (var i = 0; i < data.length; i++) {
                terminal_ids.push(data[i].terminal_id);
                if (data[i].state == 0) {
                    layer.msg("不可勾选不在线的终端!", {icon: 2})
                    return
                }
            }

            layer.confirm('确定立即重启勾选的终端吗？', function (index) {
                var postData = {terminal_ids: terminal_ids};
                server_api.rebootTerminals(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        layer.msg("终端已经在重启中，请稍候!",{icon:1})
                        setCurrentData()
                    }else{
                        layer.msg("立即重启操作失败!",{icon:2})
                    }
                });

                layer.close(index);
            });
        }else if(event=="doSearch"){
            search_key = $("input[name='search']").val()
            setCurrentData("yes")
            // $(".layui-laypage-skip").find("input").val(1);
            // $(".layui-laypage-btn").click();
        }
    })


    //监听行工具事件
    table.on('tool(terminalList)', function (obj) {
        var data = obj.data;
        if(obj.event=="incr"||obj.event=="dec"){
            if (data.state == 0) {
                layer.msg("设备不在线，不可调整音量!", {icon: 2})
                return
            }else{
                var terminal_ids = [data.terminal_id];
                var postData = {
                    terminal_ids: terminal_ids,
                    action: obj.event
                }
                server_api.changeTerminalVolume(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        var updateItem ={
                            volume:resp.result[0][data.ip]
                        }
                        obj.update(updateItem)
                        layer.msg("音量操作成功!", {icon: 1})

                    } else {
                        layer.msg("音量操作失败!", {icon: 2})
                    }
                });
            }
        }else if (obj.event === 'del') {//单项删除

            if (data.state >= 1) {
                layer.msg("不可以删除在线的终端!", {icon: 2})
                return
            } else {
                layer.confirm('删除此终端，如果其再次上线会自动增加！', function (index) {
                    var terminal_ids = [];
                    terminal_ids.push(data.terminal_id);
                    var postData = {terminal_ids: terminal_ids};
                    server_api.delTerminal(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            obj.del();
                            layer.msg("删除成功!",{icon:1})
                            addOrDelItem("del", terminal_ids.length);
                            setCurrentData("yes");

                        }else{
                            layer.msg(resp.msg,{icon:2})
                        }
                    });

                    layer.close(index);
                });
            }
        } else if (obj.event === "view-video") {
            if (data.state == 0) {
                layer.msg("终端不在线！", {icon: 2})
                return
            } if(data.terminal_id.substring(3,5).indexOf("02")!=-1){
                layer.msg("广播终端无法查看视频！", {icon: 0})
                return
            }else if(data.terminal_id.substring(3,5).indexOf("03")!=-1){
                layer.msg("音箱终端无法查看视频！", {icon: 0})
                return
            }
            var postData = {
                terminal_id: data.terminal_id,
                need_agency: 1,
                action: "start"
            }

            function stopViewVedio(stream_session){
                var postData1 = {
                    terminal_id: data.terminal_id,
                    need_agency: 1,
                    action: "stop",
                    stream_session:stream_session
                }

                server_api.getTerminalVideo(JSON.stringify(postData1), function (resp) {
                    if(resp.status!=0){
                        console.log("stop stream failed!!",resp)
                    }else{
                        console.log("stop stream ok!!",resp)
                    }
                })
            }

            server_api.getTerminalVideo(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    var title = "调取终端(" + data.ip + ")" + "视频" + ",终端安装地址:" + data.install_addr
                    var videoUrl = resp.result[0]["pull_url"]
                    var stream_session = resp.result[0]["stream_session"]
                    var params = "videoUrl=" + videoUrl + "&type=rtmp/flv" + "&stream_session=" + stream_session
                    showViewVedio(title, '../video/play.html?' + params,['90%','90%'],function () {
                        stopViewVedio(stream_session)
                    });
                    // popWindow(title,null,['90%','90%'], '../video/play.html?' + params,function () {
                    //     stopViewVedio(stream_session)
                    // })
                } else {
                    layer.msg("调取视频失败!", {icon: 2})
                }
            })
        } else if (obj.event == "bind-camera") {
            var postData1={
                terminal_id:data.terminal_id,
                page:1,
                pagesize:999
            }
            server_api.getCameraList(JSON.stringify(postData1),function (resp) {
                if(resp.status==0||resp.msg=="当前没有任何数据"){
                    var bindCameras = []
                    var cameraDiv = $("#cameraSec-1").clone();
                    if (bindCameras) {
                        $("#bindCameraForm").find("[id^='cameraSec']").remove();
                    }
                    if(resp.result){
                        bindCameras = resp.result
                    }
                    var timeWin = popWindow("绑定外接摄像头", "#bindCamera", ["1000px", "350px"])
                    function reNameCamereId() {
                        var cameraSecList = $("#bindCameraForm").find("[id^='cameraSec']");
                        if(cameraSecList==null||cameraSecList.length==0){
                            return
                        }

                        for (var i = 1; i <= cameraSecList.length; i++) {
                            $(cameraSecList[i-1]).attr("id","cameraSec-" + i)
                            $(cameraSecList[i-1]).find("[name='lable-sec']").text("摄像头" + i)
                        }
                    }
                    function doBindCamera(){
                        $("#submit-camera").off("click");
                        $("#submit-camera").on("click", function (tmpObj) {
                            var cameraSecList = $("#bindCameraForm").find("[id^='cameraSec']");
                            if(cameraSecList==null||cameraSecList.length==0){
                                layer.msg("请新增摄像头!",{icon:0})
                                return
                            }
                            var postDataList = []
                            for (var i = 1; i <= cameraSecList.length; i++) {
                                var cameraIP = "[name='" + 'cameraip-' + i + "']"
                                var channel = "[name='" + 'channel-' + i + "']"
                                var camaraName =  "[name='" + 'cameraname-' + i + "']"

                                // if($("#bindCameraForm").find(cameraIP).length == 0){
                                //     continue
                                // }

                                var postData = {
                                    camera_ip: $("#bindCameraForm").find(cameraIP).val(),
                                    channel: $("#bindCameraForm").find(channel).val(),
                                    terminal_id: data.terminal_id,
                                    name:$("#bindCameraForm").find(camaraName).val()
                                };

                                var ipCheck =  /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
                                if(!ipCheck.test(postData.camera_ip)){
                                    layer.msg("第"+i+"个摄像头IP不对!",{icon:0})
                                    return
                                }
                                var channelCheck = /\d+/
                                if(!channelCheck.test(postData.camera_ip)){
                                    layer.msg("第"+i+"个摄像头channel不对!",{icon:0})
                                    return
                                }

                                if(postData.name==null||postData.name.length==0){
                                    layer.msg("第"+i+"个摄像头未命名!",{icon:0})
                                    return
                                }

                                postDataList.push(postData)
                            }

                            for(var i=0;i<postDataList.length;i++){
                                var failedCnt =0
                                server_api.cameraAddorUpdate(JSON.stringify(postDataList[i]), function (resp) {
                                    if (resp.status == 0) {
                                        layer.msg("终端绑定外接摄像头成功!", {icon: 1})
                                        layer.close(timeWin)
                                    } else {
                                        failedCnt ++
                                    }
                                    if(failedCnt==cameraSecList.length　&& failedCnt >0){
                                        layer.msg("终端绑定外接摄像头失败"+failedCnt + "个!", {icon: 2})
                                    }
                                });
                            }
                        })
                    }

                    var addCameraBtn = $("#addCameraBtn");

                    for (var i = 1; i < bindCameras.length + 1; i++) {
                        var divClone = cameraDiv.clone();
                        var thisCamera = bindCameras[i - 1]
                        var divId = "cameraSec-" + i;
                        divClone.attr("id",divId)
                        divClone.find("[name='lable-sec']").text("摄像头" + i);
                        divClone.find("[name^='cameraip']").attr("id", "cameraip-" + i).attr("name", "cameraip-" + i).removeAttr("lay-key");
                        divClone.find("[name^='cameraip']").val(thisCamera.camera_ip);
                        divClone.find("[name^='cameraip']").attr("disabled",true)
                        divClone.find("[name^='channel']").attr("id", "channel-" + i).attr("name", "channel-" + i).removeAttr("lay-key");
                        divClone.find("[name^='channel']").val(thisCamera.channel);
                        divClone.find("[name^='channel']").attr("disabled",true)

                        divClone.find("[name^='cameraname']").attr("id", "cameraname-" + i).attr("name", "cameraname-" + i).removeAttr("lay-key");
                        divClone.find("[name^='cameraname']").val(thisCamera.name);
                        divClone.find("[name^='cameraname']").attr("disabled",true)

                        divClone.find("[name^='delCameraBtn-1']").attr("id", "delCameraBtn-" + i).attr("name", "delCameraBtn-" + i).text("解绑");
                        divClone.show()
                        //divClone.find("[name^='delCameraBtn']").attr("onclick", "onCameraSecDel(this);");
                        divClone.find("[name^='delCameraBtn']").off("click");
                        divClone.find("[name^='delCameraBtn']").on("click", function (theObj) {
                            var theThis = this
                            var totalCamera = $("#bindCameraForm").find("[id^='cameraSec']").length
                            var cameraIPSelect = "[name^='cameraip-']"
                            var channel =$(theThis).parent().find( "[name^='channel']").val()
                            var cameraIP = $(theThis).parent().find(cameraIPSelect).val()
                            //var cameraname = $(theThis).parent().find("[name^='cameraname']").val()
                            if(cameraIP==null||cameraIP.length==0){
                                layer.msg("不存在需要解绑的摄像头!", {icon: 2})
                                return
                            }

                            var postData = {
                                terminal_id: data.terminal_id,
                                camera_ip: cameraIP,
                                channel:channel
                            }
                            layer.confirm("删除将解绑该摄像头和此终端!", function (index) {
                                server_api.cameraUnbind(JSON.stringify(postData), function (resp) {
                                    if (resp.status == 0) {
                                        layer.msg("解绑成功!", {icon: 1})
                                        $(theThis).parent().remove();
                                        form.render()
                                        reNameCamereId()
                                    } else {
                                        layer.msg("解绑失败!", {icon: 2})
                                    }
                                })
                                layer.close(index);
                            })
                        })
                        divClone.id = divId;
                        addCameraBtn.before(divClone)
                    }

                    form.render()

                    doBindCamera()

                }else{
                    layer.msg("获取绑定摄像头列表失败!",{icon:2})
                }

            })

        }else if(obj.event==="rebootNow"){
            if(data.state==0){
                layer.msg("该终端已经离线，不可重启!",{icon:2})
                return
            }
            layer.confirm("立即重启该终端可能耗费几分钟，请耐心等待!", function (index) {
                var postData ={
                    terminal_ids:[data.terminal_id]
                }
                server_api.rebootTerminals(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        layer.msg("终端已经在重启，请稍等!", {icon: 1})
                    } else {
                        layer.msg("重启终端失败!", {icon: 2})
                    }
                })
                layer.close(index);
            })
        }
    });


    $("#addCameraBtn").on("click", function (object) {
        var camareDiv = $("#cameraSec-1").clone();
        var camereSecCnt = $("#bindCameraForm").find("[id^='cameraSec']").length;
        var newId = camereSecCnt + 1;

        var divId = "cameraSec-" + newId;
        var randId = new Date().getTime();
        camareDiv.attr("id",divId);

        camareDiv.find("[name='lable-sec']").text("摄像头" + newId);
        camareDiv.find("[name='cameraip-1']").attr("name", "cameraip-" + newId).attr("id", "cameraip-" + newId).removeAttr("lay-key").val("");
        camareDiv.find("[name='cameraip-1']").attr("disabled", false)
        camareDiv.find("[name='channel-1']").attr("name", "channel-" + newId).attr("id", "channel-" + newId).removeAttr("lay-key").val("");
        camareDiv.find("[name='channel-1']").attr("disabled", false)

        camareDiv.find("[name='cameraname-1']").attr("name", "cameraname-" + newId).attr("id", "cameraname-" + newId).removeAttr("lay-key").val("");
        camareDiv.find("[name='cameraname-1']").attr("disabled", false)

        camareDiv.find("[name^='delCameraBtn']").text("删除")
        camareDiv.id = divId;
        camareDiv.show()
        $(this).before(camareDiv);

        camareDiv.find("[name^='delCameraBtn']").on("click", function (theObj) {
            $(this).parent().remove();
        })
        form.render()
    });

    form.verify({
        ip: [
            /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
            , 'IP地址不符合规范'
        ]
    });

    //监听单元格编辑
    table.on('edit(terminalList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段
        var postData = {
            terminal_id: data.terminal_id,
            name: data.name,
            install_addr: data.install_addr
        }

        server_api.terminalUpdateInfo(JSON.stringify(postData), function (resp) {
            if (resp.status == 0) {
                layer.msg("修改成功!", {icon: 1});
            } else {
                setCurrentData();
                layer.msg("修改失败!", {icon: 2});
            }

        });
    });


    function popChangeGroupWin(action) {
        var data = table.checkStatus('terminalList').data;
        if (data && data.length > 0) {
            titles = {
                "move": '将勾选的终端移动到以下分组:',
                "copy": '将勾选的终端复制到以下分组:'
            }

            localStorage.setItem(action + ":terminals", JSON.stringify(data));
            if (currentPopIndex && currentPopIndex > 0) {
                layer.close(currentPopIndex);
            }
            currentPopIndex = layer.open({
                type: 1,
                title: titles[action],
                area: ['500px', '350px'],
                shadeClose: true, // 点击遮罩关闭
                content: $('#changeGrp'),
            });

            $("#popGroupList").empty()

            for (var i = 0; i < groupList.length; i++) {
                if (groupList[i].gid != currentGrp) {
                    if (action == "move") {
                        $("#popGroupList").append("<option " + "value=move:" + groupList[i].gid + ">" + groupList[i].group_name + "</option>");
                    } else {
                        $("#popGroupList").append("<option " + "value=copy:" + groupList[i].gid + ">" + groupList[i].group_name + "</option>");
                    }
                    form.render("select");
                }
            }

        } else {
            layer.msg("未勾选任何终端！", {icon: 0});
        }
    }

    function doChangeVolume(action,obj) {
        // JSONArray terminalIds = jsonBody.getJSONArray("terminal_ids");
        // String action = jsonBody.getString("action");
        var data = table.checkStatus('terminalList').data;
        if (data && data.length > 0) {
            var terminal_ids = [];
            for (var i = 0; i < data.length; i++) {
                var terminalItem = data[i];
                if (terminalItem.state == 0) {
                    layer.msg("请勿选择未在线的终端!", {icon: 2})
                    return;
                }
                terminal_ids.push(terminalItem.terminal_id);
            }
            var postData = {
                terminal_ids: terminal_ids,
                action: action
            }
            server_api.changeTerminalVolume(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("音量操作成功!", {icon: 1})
                    setCurrentData()
                } else {
                    layer.msg("音量操作失败!", {icon: 2})
                }
            });
        } else {
            layer.msg("未勾选任何终端！", {icon: 0});
        }
    }

    function cleanTerminalTask() {
        var data = table.checkStatus('terminalList').data;
        if (data && data.length > 0) {
            var terminal_ids = [];
            for (var i = 0; i < data.length; i++) {
                var terminalItem = data[i];
                if (terminalItem.state == 0) {
                    layer.msg("请勿选择未在线的终端!", {icon: 2})
                    return;
                }
                terminal_ids.push(terminalItem.terminal_id);
            }
            var postData = {
                terminalIds: terminal_ids,
                task_type:10
            }

            var treeDataList =[]
            var tmpMap ={}

            server_api.getTerminalTask(JSON.stringify(postData),function (resp) {
                if(resp.status==0){
                    var taskList = resp.result
                    for(var n=0;n<data.length;n++){
                        var terminalIp = data[n].ip
                        var oneGroup1 = {}
                        oneGroup1.title = "终端(" + terminalIp + ")"
                        oneGroup1.id = n
                        oneGroup1.ip = terminalIp
                        oneGroup1.terminal_id = data[n].terminal_id
                        oneGroup1.nTask = 0
                        oneGroup1.children =[]
                        oneGroup1.disabled = false
                        treeDataList.push(oneGroup1)
                        tmpMap[terminalIp] = n
                    }

                    for(var m=0;m<taskList.length;m++){
                        var oneTask = {}
                        var title =""
                        if(taskTypeMap[taskList[m].task_type]){
                            title +=taskTypeMap[taskList[m].task_type]
                        }else{
                            title +="其他类型"
                        }
                        title +="|"
                        if(taskList[m].task_name){
                            title += taskList[m].task_name
                        }else if(taskList[m].title){
                            title += taskList[m].title
                        }

                        if(taskList[m].start_date && taskList[m].end_date){
                            title +="("　+taskList[m].start_date +"开始,"+taskList[m].end_date +"结束)"
                        }

                        oneTask.id = m
                        oneTask.title = title
                        oneTask.task_id = taskList[m][taskTableMap[taskList[m].table_name]]
                        oneTask.table_name = taskList[m].table_name
                        oneTask.checked = false

                        treeDataList[tmpMap[taskList[m].terminal_ip]].children.push(oneTask)
                        treeDataList[tmpMap[taskList[m].terminal_ip]].nTask++
                        treeDataList[tmpMap[taskList[m].terminal_ip]].disabled = false
                    }

                    tree.render({
                        id: 'taskListTree'
                        ,elem: '#taskListTree'
                        ,data: treeDataList
                        ,isJump: true  //link 为参数匹配
                        ,showCheckbox: true
                    });

                    var winIndex =popWindow("终端任务列表!", "#taskList", ["90%", "80%"]);
                    $("#deleteTaskAction").off("click");
                    $("#deleteTaskAction").on("click",function (object) {
                        var checkedData = tree.getChecked('taskListTree'); //获取选中节点的数据
                        var failedCnt = 0
                        var tmpCnt = 0
                        for(var j=0;j<checkedData.length;j++){
                            var postData2 ={}
                            var task_ids =[]
                            var children = checkedData[j].children
                            for(var k=0;k<children.length;k++){
                                task_ids.push(children[k].task_id)
                            }
                            postData2.task_ids = task_ids
                            postData2.terminal_ip = checkedData[j].ip
                            postData2.terminal_id = checkedData[j].terminal_id

                            if(task_ids.length >0){
                                server_api.stopTerminalTask(JSON.stringify(postData2),function (resp) {
                                    tmpCnt++
                                    if(resp.status==0){
                                        failedCnt += resp.result.length
                                    }else{
                                        layer.msg("操作失败！原因:" + resp.msg, {icon: 2});
                                    }
                                    if(tmpCnt==checkedData.length){
                                        if(failedCnt >0){
                                            layer.msg("有"+failedCnt + "个任务删除失败!", {icon: 2});
                                        }else{
                                            layer.msg("删除终端任务成功!", {icon: 1});
                                            layer.close(winIndex)
                                            setTimeout(function () {
                                                setCurrentData()
                                            },6000)
                                        }
                                    }
                                })
                            }else{
                                layer.msg("未勾选任何终端任务!",{icon:0})
                            }
                        }
                    })

                    $("#deleteAllTask").off("click");
                    $("#deleteAllTask").on("click",function (object) {
                        var checkedData = tree.getChecked('taskListTree'); //获取选中节点的数据
                        var terminal_ids =[]
                        if(checkedData.length >0){
                            for(var j=0;j<checkedData.length;j++){
                                terminal_ids.push(checkedData[j].terminal_id)
                            }

                            var postData ={clean_type:"task_queue",terminal_ids:terminal_ids}
                            server_api.cleanTask(JSON.stringify(postData),function (resp) {
                                if(resp.status==0){
                                    layer.msg("清空" + terminal_ids.length +"个终端成功!",{icon:1})
                                    layer.close(winIndex)
                                }else{
                                    layer.msg(resp.msg,{icon:2})
                                }
                            })
                        }else{
                            layer.msg("请勾选终端!",{icon:0})
                        }
                    })

                }else if(resp.status==2){
                    layer.msg("操作失败！选择的终端没有任何任务！", {icon: 0});
                } else{
                    layer.msg("操作失败！原因:" + resp.msg, {icon: 2});
                }
            })

        } else {
            layer.msg("未勾选任何终端！", {icon: 0});
        }

    }


    $("#refreshGroups").on("click",function (object) {
        initGroupList()
       　layer.msg('正在更新分组信息……', {icon: 16, shade: 0.01, shadeClose: false, time: 2000});
    })

    window.searchChange = function(val) {
        if(val==null||val.length==0){
            search_key = ""
            setCurrentData("yes")
        }
    }


    function initGroupList() {
        var postData = {
            page: 1,
            pagesize: 999
        }

        server_api.getTerminalGrp(JSON.stringify(postData), function (resp) {
            if (resp.status == 0) {
                groupList = resp.result;
                $("#groupSelect").empty()
                if(currentUser.is_supper==0){
                    currentGrp = groupList[0].gid
                }

                if(currentUser && currentUser.is_supper==1){
                    $("#groupSelect").append("<option " + "value=" + "all-group" + ">" + "所有分组" + "</option>");
                }
                for (var i = 0; i < groupList.length; i++) {
                    $("#groupSelect").append("<option " + "value=" + groupList[i].gid + ">" + groupList[i].group_name + "</option>");
                    form.render("select");
                }
            } else {
                layer.msg("获取终端分组失败!", {icon: 2});
            }
            setCurrentData("yes");
        });
    }


    function autoRefreshTerminalList() {
        setTimeout(function () {
           var isAutoRefresh =  $("#isAutoRefresh").is(":checked")
            if(isAutoRefresh){
                setCurrentData("yes")
            }
            autoRefreshTerminalList()
        },30000)
    }

    form.on('checkbox(autoRefresh)', function(checkObj){
        var isAutoRefresh = $("#isAutoRefresh").is(":checked");
        if(isAutoRefresh){
            layer.msg("每间隔30秒钟将会自动刷新列表一次!",{icon:0})
        }
    });


    $(function () {

        //开始获取数据
        server_api.getUserInfo(null, function (resp) {
            if (resp.status == 0) {
                currentUser = resp.result[0]

                initGroupList();

                autoRefreshTerminalList()
            }else{
                layer.msg("请重新登录!",{icon:2})
            }
        })

    });
});