layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api',
    common_api: 'common_api'
});
var planType ="exam"
var currentPlanId = null
var weeks = ["周一","周二","周三","周四","周五","周六","周日"]

var taskTypeMap = {
    1: "文本类型",
    2: "图片类型",
    3: "音频类型",
    4: "视频类型"
}

var playModeMap = {
    1: "每天循环",
    2: "按周循环",
    3: "不循环"
}

var priorityMap = {
    1: "最低级",
    2: "较低级",
    3: "中级",
    4: "较高级",
    5: "最高级"
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null,
    plan_type: "normal"
}

var newExamTask ={
    plan_type:"exam",
    task_name:"新的测试任务（请修改此任务)",
    task_type:3,
    priority:5,//默认最低级
    start_date:"2019-09-03",
    end_date:"2019-09-03",
    play_mode:1,//每天循环
    week_days:[],
    play_periods:[{"from":"09:00","to":"11:00",playcount:0}],
    content:["请修改","请修改"]
}

var TerminalGrpList =null
var AllTerminalList =null
var currentExamInfo = null


layui.use(['jquery', 'form','tree', 'table', 'transfer','util','admin', 'laypage', 'laydate', 'server_api','common_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        transfer =layui.transfer,
        util = layui.util,
        tree = layui.tree,
        common_api = layui.common_api,
        server_api = layui.server_api;
    var verifyMap = {
        //数组的两个值分别代表：[正则匹配、匹配不符时的提示文字]
        exam_code: [/^[a-zA-Z][a-zA-Z0-9]*$/,'分组编号只能包含字母和数字,以字母开头!']
    }

    form.verify(verifyMap);

    form.render()
    //全局变量区域

    function checkTaskPeriod(examInfo,taskPeriod) {
        var isAMorPM = 0 // 0为既不是上午，也不是下午，1为上午,2为下午
        var tmpTimeSpace =5*60*1000
        var amstartTime = examInfo.start_date + " " + examInfo.am_start_time + ":00"
        var amendTime = examInfo.start_date + " " + examInfo.am_end_time + ":59"
        var pmstartTime = examInfo.start_date + " " + examInfo.pm_start_time + ":00"
        var pmendTime = examInfo.start_date + " " + examInfo.pm_end_time + ":59"

        var checkTimeFrom =  examInfo.start_date + " " + taskPeriod.from + ":00"
        var checkTimeTo =  examInfo.start_date + " " + taskPeriod.to + ":00"

        if(new Date(checkTimeFrom).getTime() >= (new Date(amstartTime).getTime()-tmpTimeSpace) && new Date(checkTimeTo).getTime() <= (new Date(amendTime).getTime() + tmpTimeSpace)){
            isAMorPM = 1
        }else if(new Date(checkTimeFrom).getTime() >= (new Date(pmstartTime).getTime()-tmpTimeSpace) && new Date(checkTimeTo).getTime() <= (new Date(pmendTime).getTime() + tmpTimeSpace)){
            isAMorPM = 2
        }

        return isAMorPM
    }

    function getRowDataByTaskId(taskId) {
        var row = null
        if(pageData.dataList){
            for(var i=0;i<pageData.dataList.length;i++){
                if(pageData.dataList[i].task_id == taskId){
                    row =  pageData.dataList[i]
                    break
                }
            }
        }
        return row
    }

    function updateRowDataByTaskId(taskId,item,value) {
        if(pageData.dataList){
            for(var i=0;i<pageData.dataList.length;i++){
                if(pageData.dataList[i].task_id == taskId){
                    pageData.dataList[i][item] = value
                    break
                }
            }
        }
        return
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
                if (pageData.total > 0) {
                    setCurrentData();
                }
            }
        });
    }


    function renderDataList(dataList) {

        var colsList =[]

        colsList.push({checkbox: true,LAY_CHECKED:true})
        colsList.push({type: "numbers",title: '序号',width: 80,})
       // colsList.push({field: 'id', align: 'left', title: '序号', width: 80, sort: true})
        colsList.push({title:'操作',align: 'left', width:80,templet:function (val) {
            var delBtn = '<div class="layui-btn  layui-btn-xs layui-btn-danger" lay-event="deltask">删除</div>'
            return delBtn
            }})

        colsList.push({
            field: 'task_name',
            align: 'left',
            title: '任务名称(可编辑)',
            width: 300,
            edit: 'text',
            event: 'task_name'
        })

        colsList.push( {
            field: 'ready_terminals',
            align: 'center',
            title: '向终端下发状态',
            width: 160,
            event: 'termial_status',
            templet:function (val) {
                var showBtn=""
                showBtn += '<span class="layui-btn layui-btn-xs layui-btn-normal" lay-event="ready_terminals">' + val.ready_terminals.length +'个未下发</span><br>';
                if(val.ok_terminals.length >0){
                    showBtn += '<span class="layui-btn layui-btn-xs layui-btn-checked" lay-event="ok_terminals">'+val.ok_terminals.length　+'个已下发</span><br>';
                }

                if(val.fail_terminals.length >0){
                    showBtn += '<span class="layui-btn layui-btn-xs layui-btn-danger" lay-event="fail_terminals">'+val.fail_terminals.length +'个已失败(点击查看)</span>';
                }
                return showBtn
            }
        })

        colsList.push({
            field: 'task_type',
            align: 'left',
            title: '任务类型',
            width: 140,
            event: "task_type",
            templet:"#selectTaskType",
        })

        colsList.push( {
            field: 'content',
            align: 'left',
            title: '任务内容',
            width:250,
            event: "content",
            templet: function (val) {
                var showStr = "";
                if (val.task_type == 1||val.task_type == "1") {//文本类型
                    var title = "标题:" + val.content[0].substring(0, 10);
                    if (val.content[0].length > 10) {
                        title += "……"
                    }
                    var text = "内容:" + val.content[1].substring(0, 10);
                    if (val.content[1].length > 10) {
                        text += "……"
                    }
                    showStr += title;
                    showStr += "<br>";
                    showStr += text;

                } else if (val.task_type == 2||val.task_type == "2") {
                    showStr = "请点击查看和更换图片";
                } else if (val.task_type == 3||val.task_type == "3") {
                    showStr = "请点击查看和更换音频";
                } else if (val.task_type == 4||val.task_type == "4") {
                    showStr = "请点击查看和更换视频";
                }
                return showStr;
            }
        })

        colsList.push( {
            field: 'play_periods',
            // colspan: 3,
            align: 'center',
            title: '每天执行时间段',
            width: 340,
            event: "play_periods",
            templet: function (val) {
                this.rowspan = val.play_periods.length;
                var showStr = "";
                for (var i = 0; i < val.play_periods.length; i++) {
                    showStr += "开始:" + val.play_periods[i].from;
                    showStr += ", 结束:" + val.play_periods[i].to;
                    if (val.play_periods[i].playcount == 0) {
                        showStr += ", 播放次数:循环";
                    } else {
                        showStr += ", 播放次数:" + val.play_periods[i].playcount;
                    }
                    showStr += "<br>";
                }
                return showStr;
            }
        })

        colsList.push( {
            field: 'priority',
            align: 'left',
            title: '优先级',
            width: 120,
            event: "priority",
            templet:"#selectTaskPriority",
        })

        colsList.push( {field: 'create_user', align: 'center', width: 140,title: '创建人'})
        colsList.push( {field: 'create_time', align: 'center',   width: 180, title: '创建时间', templet: function (val) {
            return val.create_time.substring(0,19)
        }
        })

        var renderParam = {
            id: "taskList",//
            elem: '#taskList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            //cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count){

            },
            // parseData: function (res) { //res 即为原始返回的数据
            //     console.log("run into rable parse data");
            //     console.log(res)
            //     return res
            // },
            cols: [colsList]
        }
        renderParam.toolbar = "#toolbarAction"
        table.render(renderParam);
    }

    function updateAllTaskPeroid(newTime,n) {
        var isOn = localStorage.getItem("timeRelate");
        if("on"!=isOn && newTime!=null && newTime.length >0){
            return
        }
        var allTask = common_api.simpleCopyObject(table.cache["taskList"])
        if(allTask!=null){
            if(allTask[0].play_periods[n]){
                var beginTime = allTask[0].play_periods[n].from
                var distance = common_api.timeCalculateMinute(newTime,beginTime)
                for(var i=0;i<allTask.length;i++){
                    var playPeriods = common_api.simpleCopyObject(allTask[i].play_periods)
                    // for(var n=0;n<playPeriods.length;n++){
                    //
                    // }
                    if(i==0){
                        playPeriods[n].from = newTime
                        playPeriods[n].to = common_api.timeAddValue(playPeriods[n].to,distance)
                    }else {
                        playPeriods[n].from = common_api.timeAddValue(playPeriods[n].from,distance)
                        playPeriods[n].to = common_api.timeAddValue(playPeriods[n].to,distance)
                    }
                    //allTask[i].play_periods = playPeriods
                    updateTaskInfo(allTask[i].task_id,"play_periods",playPeriods)
                }
            }
        }
    }

    function updateTaskInfo(taskId,item,value,obj){
        var data = getRowDataByTaskId(taskId)
        var current = data[item]
        if(data !=null){
            if(data.ok_terminals.length > 0){
                layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                return
            }
            var postData = data;
            if (current!= value) {
                postData[item] = value;
                server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                    if(resp.status==0){

                        var tmpPostData ={task_id:data.task_id,plan_type:planType}
                        server_api.getTaskByTaskId(JSON.stringify(tmpPostData),function (resp) {
                            if(resp.status==0){
                                updateRowDataByTaskId(data.task_id,item,value)
                                 if(obj){
                                     var updateData = resp.result[0]
                                      delete updateData.id
                                     //刷新下拉会有问题
                                     // delete updateData["task_type"]
                                     // delete updateData["play_mode"]
                                     // delete updateData["priority"]
                                     obj.update(updateData)
                                     table.reload('taskList')
                                 }else{
                                     table.reload("taskList")
                                 }
                                layer.msg("修改成功!",{icon:1})
                            }else{
                                $(".layui-icon-refresh").click()
                            }
                            //layer.close(tmpIndex)
                        })
                    }else{
                        layer.msg("修改失败!原因:"+resp.msg,{icon:2})
                    }
                });
            }else{
                console.log("the same task type:",data.task_type)
            }
        }

    }




    function setCurrentData(getTotal, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            plan_type: planType
        }

        if(getTotal && getTotal.length >0){
            postData.getTotal = getTotal;
        }

        if(currentPlanId && currentPlanId.length >0){
            postData.plan_id = currentPlanId;
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getTaskList(JSON.stringify(postData), function (resp) {
            var dataList = [];
            if (resp.status == 0) {
                //refreshCurrentPage(table,refreshResp.result);
                dataList = resp.result;
                pageData.dataList = dataList;
                if (getTotal == "yes") {
                    renderPages(resp.total);
                }
            } else {
                renderPages(0);
                //layer.msg(resp.msg, {icon: 2});
            }
            renderDataList(dataList);
        });
    }

    //完整功能

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
            success: function(layero, index) {
               //layer.iframeAuto(index);
            },
            yes: function (index, layero) {
                //do something
                console.log("run into yes!")
            },
            cancel: function () {

            }
        });
       //layer.iframeAuto(index)
       return index;
    }

    function initTransfer(obj){
        var data = obj.data;
        var postData ={
            page:1,
            pagesize:99999,
            getTotal:"yes",
            file_type:data.task_type-1
        }

        var bindElem = ''
        var titles = []
        if(data.task_type==2){
            bindElem = '#mediaTransfer'
            titles = ['图片文件列表', '任务图片列表']
        }else　if(data.task_type==3){
            bindElem = '#mediaTransfer'
            titles = ['音频文件列表', '任务音频列表']
        }else if(data.task_type==4){
            bindElem = '#mediaTransfer'
            titles = ['视频文件列表', '任务视频列表']
        }

        server_api.getFileList(JSON.stringify(postData),function (resp) {
            if(resp.status==0){
                var winTitle = ""
                var winIndex = 0;

                if(data.task_type ==2){
                    winTitle = "修改任务图片内容";
                    $("#modifyMediaContent").find("legend").text("请选择图片文件(在左侧附件管理菜单可上传)")

                }else if(data.task_type ==3){
                    winTitle = "修改任务音频内容";
                    $("#modifyMediaContent").find("legend").text("请选择音频文件(在左侧附件管理菜单可上传)")
                }else if(data.task_type ==4){
                    winTitle = "修改任务视频内容";
                    $("#modifyMediaContent").find("legend").text("请选择视频文件(在左侧附件管理菜单可上传)")
                }

                winIndex = popWindow(winTitle, "#modifyMediaContent", ["80%", "70%"]);

                var mediaList =[];
                var taskMediaList =[];
                //var isInRight = false
                for(var i=0;i<resp.result.length;i++){
                    var imageItem ={}
                    var content = data["content"];
                    //isInRight = false
                    imageItem.value = resp.result[i].attach_id;
                    imageItem.title = resp.result[i].name;
                    for(var j=0;j<content.length;j++){
                        if(content[j]==imageItem.value){
                            taskMediaList.push(imageItem.value)
                           // isInRight = true
                            break
                        }
                    }
                    mediaList.push(imageItem);
                }


                transfer.render({
                    id:bindElem.replace("#","")
                    ,elem: bindElem
                    ,data: mediaList
                    ,title: titles
                    ,showSearch: true
                    ,width:'35%'
                    ,height:380
                    ,value: taskMediaList
                    ,onchange: function(obj, index){

                    }
                })


                var tmpHeight = $("#mediaTransfer").height()
                $("#modifyMediaContent").find("textarea").css("height",tmpHeight)
                $("#modifyMediaContent").find("textarea").val("")
                $("#modifyMediaContent").find("textarea").val(content[1])
                $("#modifyMediaContent").find('[id=submitMediaContent]').off("click");
                $("#modifyMediaContent").find('[id=submitMediaContent]').on("click",function (object) {
                    var getData = transfer.getData(bindElem.replace("#",""));
                    if(getData.length==0){
                        layer.msg("任务文件列表不允许为空!",{icon:0})
                        return
                    }else if(data.ok_terminals.length >0){
                        layer.msg("任务存在启用的终端,不可修改!",{icon:0})
                        return
                    }
                    layer.close(winIndex);
                    var postData = data;
                    var content = [""]; //第一代表标题
                    content.push($("#noticeTextArea").val()) //滚动字幕内容
                    for(var mediaId in getData){
                        content.push(getData[mediaId].value)
                    }


                    postData.content = content;
                    server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                        if(resp.status==0){
                            var newdata ={}
                            newdata.content = content;
                            obj.update(newdata);
                            layer.msg("修改任务文件列表成功!",{icon:1})
                        }else{
                            layer.msg("修改任务文件列表失败!",{icon:2})
                        }
                    });
                })
            }else{
                layer.msg("请先在附件管理上传文件!",{icon:0})
            }

        })

    }

    //头工具栏事件
    table.on('toolbar(taskList)', function(obj){
        switch(obj.event){
            case 'batchDelTask':
                var data = table.checkStatus('taskList').data;
                if (data && data.length > 0) {
                    layer.confirm('是否真的删除勾选的任务?', {icon: 3, title:'提示'}, function(index){
                        //do something
                        var task_ids = []
                        for(var i=0;i<data.length;i++){
                            task_ids.push(data[i].task_id)
                            if(data[i].ok_terminals.length >0){
                                var showIndex =i +1
                                layer.msg("第" + showIndex+"个任务存在启用的终端,无法删除!",{icon:0})
                                return
                            }
                        }
                        var postData={
                            task_ids:task_ids
                        }

                        if(planType=="exam"){
                            postData.plan_type = "exam"
                            postData.plan_id = data.plan_id
                        }
                        server_api.delTask(JSON.stringify(postData),function (resp) {
                            if(resp.status==0){
                                //obj.del();
                                layer.msg("删除成功!",{icon:1})
                                addOrDelItem("del",data.length)
                                setCurrentData("yes")
                            }else{
                                layer.msg(resp.msg,{icon:2})
                            }
                        })
                    });

                }else{
                    layer.msg("未勾选任何任务!",{icon:0})
                }
                break;
            case 'addTaskBtn':
                layer.confirm('确定增加新的任务?', {icon: 3, title:'提示'}, function(index){
                    var tmpTask = null
                    tmpTask = newExamTask
                    tmpTask.plan_id = currentPlanId
                    tmpTask.plan_type = planType
                    tmpTask.task_name ="新的测试任务（请修改此任务)" +  new Date().getTime()
                    server_api.addOrUpdateTask(JSON.stringify(tmpTask),function (resp) {
                        if(resp.status==0){
                            layer.msg("新增任务成功!请在列表中进行设置!",{icon:1})
                            addOrDelItem("add",1)
                            setCurrentData("yes")
                        }else{
                            layer.msg("新增任务失败!,可能与其他定时任务或临时任务重名！",{icon:2})
                        }
                    })
                })
                break;
            case 'exportTaskBtn':
                var data = table.checkStatus('taskList').data;
                if (data && data.length > 0) {
                    layer.confirm('是否真的导出任务以及任务媒体文件?', {icon: 3, title:'提示'}, function(index) {
                        //do something
                        var task_ids = []
                        for (var i = 0; i < data.length; i++) {
                            task_ids.push(data[i].task_id)
                        }
                        var postData ={
                            task_ids:task_ids
                        }

                        server_api.exportTask(JSON.stringify(postData),"boyao_task_export_" + new Date().getTime() + ".zip")
                        layer.close(index)

                    })

                }else{
                    layer.msg("未勾选任何任务!",{icon:0})
                    return
                }
                break

            case 'batchSendTask':
                var data = table.checkStatus('taskList').data;
                if(data.length==0){
                    layer.msg("未勾选任何考试任务!",{icon:0})
                    return
                }else if(currentExamInfo!=null){
                    var start_date = currentExamInfo.start_date
                    if(!currentExamInfo
                        ||!currentExamInfo.start_date
                        ||!currentExamInfo.end_date){
                        layer.msg("请先设置考试日期和时间！",{icon:0})
                        return
                    }
                    var isValideData = common_api.diffWithToday(start_date)
                    if(isValideData <0){
                        layer.msg("考试日期已经过期！",{icon:0})
                        return
                    }

                    var task_ids = []
                    var oldTerminals =[]
                    for(var i=0;i<data.length;i++){
                        var tmpIndex = i+1
                        if(data[i].task_type >1 && data[i].content.length <=2){
                            var errMsg = "第"+tmpIndex + "行任务媒体文件列表为空！"
                            layer.msg(errMsg,{icon:0})
                            return
                        }else if(data[i].task_type >1 && data[i].content[2].length ==0){
                            var errMsg = "第"+tmpIndex + "行任务媒体文件列表为空！"
                            layer.msg(errMsg,{icon:0})
                            return
                        }
                        var playPeroid = data[i].play_periods
                        for(var n=0;n<playPeroid.length;n++){
                            var index = i+1
                            var regCheck = /^[0-9]{2}:[0-9]{2}/
                            if(!regCheck.test(playPeroid[n].from)||!regCheck.test(playPeroid[n].to)){
                                layer.msg("第"+index+"个任务的执行时间设置有问题!请修改",{icon:0})
                                return
                            }else if(isNaN(playPeroid[n].playcount)){
                                layer.msg("第"+index+"个任务的执行时间设置有问题!请修改",{icon:0})
                                return
                            }

                            if(0 ==checkTaskPeriod(currentExamInfo,playPeroid[n])){
                                layer.msg("第"+index+"个任务的执行时间不在考试时间内!请修改",{icon:0})
                                return
                            }
                        }
                        task_ids.push(data[i].task_id)
                        oldTerminals.push(...data[i].ready_terminals)
                    }
                    sendTaskDirectly(task_ids,oldTerminals)
                }else{
                    layer.msg("获取考试信息失败！",{icon:2})
                }
                break
            case 'batchStopTask':
                var data = table.checkStatus('taskList').data;
                if(data.length==0){
                    layer.msg("未勾选任何考试任务!",{icon:0})
                    return
                }else{
                    var postData = {}
                    layer.confirm("是否真的停用勾选的任务?",{icon: 3, title:'提示'},function (index) {
                        var loadingFlag = layer.msg('正在下发清除指令到各个终端……', {
                            icon: 16,
                            shade: 0.01,
                            shadeClose: false,
                            time: 0
                        });
                        var failedTasks = []
                        var doCount = 0
                        function doStopTask() {
                            //发送屏蔽任务
                            var postShield ={exam_id:currentPlanId,action:0}
                            server_api.startOrStopShieldForExam(JSON.stringify(postShield),function (resp) {
                                if(resp.status!=0){
                                    layer.msg("屏蔽器任务停止失败!",{icon:2})
                                }

                                for(var i=0;i<data.length;i++) {
                                    if (data[i].ok_terminals.length == 0) {
                                        layer.msg("不能勾选启用终端为0的任务!", {icon: 0})
                                        return
                                    }
                                    postData = {
                                        task_id: data[i].task_id
                                    }
                                    server_api.stopTask(JSON.stringify(postData), function (resp) {
                                        if (resp.status == 0) {
                                            var updateItem = {}
                                            updateItem.state = 0
                                            //data[i].update(updateItem)
                                        } else {
                                            failedTasks.push(postData.task_id)
                                        }
                                        doCount ++
                                        if(doCount == data.length){
                                            if(failedTasks.length >0){
                                                layer.msg(failedTasks.length + "个任务停用失败!", {icon: 2})
                                            }else{
                                                layer.msg("停用成功!", {icon: 1})
                                            }
                                            setCurrentData()
                                            layer.close(loadingFlag)
                                            layer.close(index)
                                        }
                                    })
                                }
                            })

                        }

                        var countDownTime = currentExamInfo.count_down
                        if(countDownTime!=null && countDownTime>0){
                            var tmpPost={
                                plan_id:currentPlanId,
                                plan_type:"exam",
                                action:"unset"
                            }
                            server_api.sendTimeCountDownTask(JSON.stringify(tmpPost),function (resp) {
                                if(resp.status!=0){
                                    layer.msg("取消倒计时失败!",{icon:2})
                                }else{
                                    doStopTask()
                                }
                            })
                        }else{
                            doStopTask()
                        }
                    })
                }
                break;

        };
    });

    $("#addTimeBtn").on("click", function (object) {
        var timeDiv = $("#timeSection-1").clone();
        var timeSecCnt = $("#modifyTimeForm").find("[id^='timeSection']").length;
        var newId = timeSecCnt + 1;

        var divId = "timeSection-" + newId;
        var randId = new Date().getTime();
        timeDiv.id = divId;

        timeDiv.find("[name='lable-sec']").text("时间段" + newId);
        timeDiv.find("[name='starttime-1']").attr("name", "starttime-" + newId).attr("id", "starttime-" + newId).removeAttr("lay-key").val("");
        timeDiv.find("[name='endtime-1']").attr("name", "endtime-" + newId).attr("id", "endtime-" + newId).removeAttr("lay-key").val("");
        timeDiv.find("[name='playcount-1']").val("");
        timeDiv.find("[name='playcount-1']").attr("id", "playcount-" + newId).attr("name", "playcount-" + newId);

        timeDiv.id = divId;

        $(this).before(timeDiv);

        form.render()

        // 日期范围
        $('body').find('[id^="starttime-"]').off("click");
        $('body').on('click', '[id^="starttime-"]', function(e) {
            var id = $(this).prop('id');
            var dateOptions = {
                elem: '#'+id,
                type: 'time',
                format: 'HH:mm:ss',
                // btns: ['now', 'confirm'],
                done: function(formatTime, date) {
                    // ...
                }
            };
            if($('.layui-laydate').size() === 0) {
                dateOptions['show'] = true;
            }
            laydate.render(dateOptions);
        });

        // 日期范围
        $('body').find('[id^="endtime-"]').off("click");
        $('body').on('click', '[id^="endtime-"]', function(e) {
            var id = $(this).prop('id');
            var dateOptions = {
                elem: '#'+id,
                type: 'time',
                format: 'HH:mm:ss',
                // btns: ['now', 'confirm'],
                done: function(formatTime, date) {
                    // ...
                }
            };
            if($('.layui-laydate').size() === 0) {
                dateOptions['show'] = true;
            }
            laydate.render(dateOptions);
        });

    });

    window.onTimeSecDel = function (obj) {
        if ($("#modifyTimeForm").find("[id^='timeSection']").length <= 1) {
            layer.msg("必须保留一个时间段!",{icon:0})
        } else {
            $(obj).parent().remove();
        }
    }

    //监听单元格编辑
    table.on('edit(taskList)', function(obj){
        var value = obj.value //得到修改后的值
            ,data = obj.data //得到所在行所有键值
            ,field = obj.field; //得到字段
        var data = obj.data;
        var postData = data;
        if(data.ok_terminals.length >0){
            layer.msg("任务存在启用的终端,不可修改!",{icon:0})
            setCurrentData()
            return
        }
        if(field=="task_name"){
            //console.log("run into modify task_name")
            postData.task_name = value;
            server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                if(resp.status==0){
                    var newdata = {};
                    newdata[field] = value;
                    obj.update(newdata);
                    layer.msg("修改任务名成功!",{icon:1})
                }else{
                    layer.msg("修改任务名失败!",{icon:2})
                }

            });
        }


    });

    function sendTaskDirectly(task_ids,oldTerminals){
        var treeDataList = []
        if (AllTerminalList != null) {
            var allTerminalList = AllTerminalList;
            var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
            if (TerminalGrpList != null) {
                var groupList = TerminalGrpList;
                treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,true)
                tree.render({
                    id: 'terminalGrpTree'
                    , elem: '#terminalGrpTree'
                    , data: treeDataList
                    , isJump: true  //link 为参数匹配
                    , showCheckbox: true
                });

                $("#terminalGrpTree").append($('  <div  class="align-center verical-margin">\n' +
                    '        <div id="saveTerminalAndSend" class="layui-btn">立即下发</div>\n' +
                    '    </div>'))

                $("#saveTerminalAndSend").off("click");
                $("#saveTerminalAndSend").on("click", function (object) {

                    var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                    var terminalIdList = common_api.getCheckTermialIds(checkedData)

                    if (terminalIdList.length > 0) {
                        var failedCnt = 0
                        var requestCnt =0
                        var failTaskCnt =0
                        var loadingFlag = layer.msg('正在下发任务到各个终端……', {
                            icon: 16,
                            shade: 0.01,
                            shadeClose: false,
                            time: 0
                        });
                        for(var i=0;i<task_ids.length;i++){
                            var postData = {
                                task_id:task_ids[i],
                                terminal_ids:terminalIdList
                            }
                            server_api.sendTask2terminal(JSON.stringify(postData),function (resp) {
                                requestCnt++
                                if(resp.status==0){
                                    failedCnt += resp.result[0].failedCnt
                                }else{
                                    layer.msg(resp.msg, {icon: 2})
                                    failTaskCnt++
                                }
                                if(failTaskCnt==0 && requestCnt==task_ids.length){
                                    layer.close(loadingFlag)
                                    var failMsg = ""
                                    if(failTaskCnt >0){
                                        failMsg += "有"+failTaskCnt+"个任务"
                                    }
                                    if(failedCnt >0){
                                        failMsg += "发送到"+failedCnt+"个终端失败"
                                        layer.msg(failMsg, {icon: 0})
                                    } else{
                                        layer.msg("发送成功!", {icon: 1})

                                        if(currentExamInfo!=null && currentExamInfo.count_down!=null && currentExamInfo.count_down>0){
                                            var tmpPost={
                                                plan_id:currentPlanId,
                                                plan_type:"exam",
                                                action:"set"
                                            }
                                            server_api.sendTimeCountDownTask(JSON.stringify(tmpPost),function (resp) {
                                                if(resp.status!=0){
                                                    layer.msg("设置倒计时失败!",{icon:2})
                                                }
                                            })
                                        }

                                        //发送屏蔽任务
                                        var postShield ={exam_id:currentPlanId,action:1}
                                        server_api.startOrStopShieldForExam(JSON.stringify(postShield),function (resp) {
                                            if(resp.status!=0){
                                                layer.msg("屏蔽器任务发送失败!",{icon:2})
                                            }
                                        })
                                    }
                                    setCurrentData()
                                }
                            })
                            layer.close(tmpIndex)
                        }
                    } else {
                        layer.msg("未勾选任何终端!", {icon: 0})
                    }
                    //console.log(checkedData);
                })
            }
        }else{
            layer.msg("没有获取到终端列表!",{icon:2})
        }

    }

    //监听任务表单项修改
    table.on('tool(taskList)', function (obj) {
            var data = obj.data;
            var event = obj.event;

            if(event=="deltask"){
                if(data.ok_terminals.length > 0){
                    layer.msg("任务存在启用的终端,不可以删除!",{icon:0})
                    return
                }

                layer.confirm('是否真的删除此任务?', {icon: 3, title:'提示'}, function(index){
                    //do something
                    var task_ids = []
                    task_ids.push(data.task_id)
                    var postData={
                        task_ids:task_ids
                    }

                    if(planType=="exam"){
                        postData.plan_type = "exam"
                        postData.plan_id = data.plan_id
                    }

                    server_api.delTask(JSON.stringify(postData),function (resp) {
                        if(resp.status==0){
                            obj.del();
                            layer.msg("删除成功!",{icon:1})
                            addOrDelItem("del",1)
                            setCurrentData("yes")
                        }else{
                            layer.msg(resp.msg,{icon:2})
                        }
                    })
                });
            }else if (event == "task_type") {
                // var selected = $("#tasktype-row-" + data.task_id).children('option:selected').val()
                // updateTaskInfo(data.task_id,"task_type",selected,obj)
                form.on('select(taskType)', function(selectObj){
                    var selectId = $(selectObj.elem).attr("id")
                    var taskId = selectId.split("-")[2]
                    updateTaskInfo(taskId,"task_type",selectObj.value,obj)
                });
            } else if (event == "play_mode") {
                // var selected = $("#taskplaymode-row-" + data.task_id).children('option:selected').val()
                // updateTaskInfo(data.task_id,"play_mode",selected,obj)
                form.on('select(taskPlayMode)', function(selectObj){
                    var selectId = $(selectObj.elem).attr("id")
                    var taskId = selectId.split("-")[2]

                    updateTaskInfo(taskId,"play_mode",selectObj.value,obj)
                });

            } else if (event == "priority") {
                // var selected = $("#taskpriority-row-" + data.task_id).children('option:selected').val()
                // updateTaskInfo(data.task_id,"priority",selected,obj)

                form.on('select(taskPriority)', function(selectObj){
                    var selectId = $(selectObj.elem).attr("id")
                    var taskId = selectId.split("-")[2]
                    updateTaskInfo(taskId,"priority",selectObj.value,obj)
                });

            }  else if (event == "play_periods") {
                if(data.ok_terminals.length > 0){
                    layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                    return
                }
                //buildModifyTaskTimeWin(data.play_periods);
                localStorage.setItem("modifyRecord", JSON.stringify(data));
                var periods = data.play_periods;
                var isLiandong = $("#playtime_liandong").prop("checked");
                var timeDiv = $("#timeSection-1").clone();
                $("#modifyTimeForm").find("[id^='timeSection']").remove();
                var addTimeBtn = $("#addTimeBtn");

                for (var i = 1; i < periods.length + 1; i++) {
                    var divClone = timeDiv.clone();
                   // var randId = new Date().getTime();
                    var divId = "timeSection-" + i;
                    divClone.find("[name='lable-sec']").text("时间段" + i);
                    divClone.find("[name^='starttime']").attr("id", "starttime-" + i).attr("name", "starttime-" + i).removeAttr("lay-key");
                    divClone.find("[name^='starttime']").val(periods[i - 1].from);
                    if(isLiandong){
                        divClone.find("[name^='starttime']").attr("disabled", true)
                    }else{
                        divClone.find("[name^='starttime']").attr("disabled", false)
                    }
                    divClone.find("[name^='endtime']").attr("id", "endtime-" + i).attr("name", "endtime-" + i).removeAttr("lay-key");
                    divClone.find("[name^='endtime']").val(periods[i - 1].to);
                    if(isLiandong){
                        divClone.find("[name^='endtime']").attr("disabled", true)
                    }else{
                        divClone.find("[name^='endtime']").attr("disabled", false)
                    }
                    divClone.find("[name^='playcount']").attr("id", "playcount-" + i).attr("name", "playcount-" + i);
                    divClone.find("[name^='playcount']").val(periods[i - 1].playcount);

                    divClone.find("[name^='delTimeBtn-1']").attr("id", "delTimeBtn-" + i).attr("name", "delTimeBtn-" + i);

                    divClone.find("[name^='delTimeBtn']").attr("onclick", "onTimeSecDel(this);");
                    divClone.id = divId;
                    addTimeBtn.before(divClone)

                    // 时间范围
                    $('body').on('click', '[id^="starttime-"]', function(e) {
                        var id = $(this).prop('id');
                        var dateOptions = {
                            elem: '#'+id,
                            type: 'time',
                            format: 'HH:mm',
                            // btns: ['now', 'confirm'],
                            done: function(formatTime, date) {
                                // ...
                            }
                        };
                        if($('.layui-laydate').size() === 0) {
                            dateOptions['show'] = true;
                        }
                        if(currentPlanId!=null && planType=="exam"){
                            var examData = $("#exam_startdate").val()
                            var minTime = $("#exam_start1").val()
                            var maxTime = $("#exam_end1").val()
                            if(minTime!=null && maxTime!=null){
                                var tmpDate1 = new Date(examData +" "+ minTime)
                                tmpDate1.setMinutes(tmpDate1.getMinutes() - 10, tmpDate1.getSeconds(), 0);
                                dateOptions.min = tmpDate1.getHours() + ":" + tmpDate1.getMinutes()+":00"
                                var tmpDate2 = new Date(examData +" "+ maxTime)
                                tmpDate2.setMinutes(tmpDate2.getMinutes() + 10, tmpDate2.getSeconds(), 0);
                                dateOptions.max = tmpDate2.getHours() + ":" + tmpDate2.getMinutes()+":00"
                            }
                        }
                        laydate.render(dateOptions);
                    });

                    // 时间范围
                    $('body').on('click', '[id^="endtime-"]', function(e) {
                        var id = $(this).prop('id');
                        var dateOptions = {
                            elem: '#'+id,
                            type: 'time',
                            format: 'HH:mm',
                            // btns: ['now', 'confirm'],
                            done: function(formatTime, date) {
                                // ...
                            }
                        };
                        if($('.layui-laydate').size() === 0) {
                            dateOptions['show'] = true;
                        }
                        if(currentPlanId!=null && planType=="exam"){
                            var examData = $("#exam_startdate").val()
                            var minTime = $("#exam_start1").val()
                            var maxTime = $("#exam_end1").val()
                            if(minTime!=null && maxTime!=null){
                                var tmpDate1 = new Date(examData +" "+ minTime)
                                tmpDate1.setMinutes(tmpDate1.getMinutes() - 10, tmpDate1.getSeconds(), 0);
                                dateOptions.min = tmpDate1.getHours() + ":" + tmpDate1.getMinutes()+":00"
                                var tmpDate2 = new Date(examData +" "+ maxTime)
                                tmpDate2.setMinutes(tmpDate2.getMinutes() + 10, tmpDate2.getSeconds(), 0);
                                dateOptions.max = tmpDate2.getHours() + ":" + tmpDate2.getMinutes()+":00"
                            }
                        }
                        laydate.render(dateOptions);
                    });
                }

                form.render()

                var timeWin =popWindow("修改每天播放时间段!", "#modifyTaskTime", ["80%", "50%"]);
                $("#submit-playtime").off("click");
                $("#submit-playtime").on("click",function (updatePeriods) {

                    var timeSecCnt = $("#modifyTimeForm").find("[id^='timeSection']").length;
                    var newPeriods = []
                    for(var i=1;i<=timeSecCnt;i++){
                        var onePlayTime = {}
                        var startTimeName = "[name='" + 'starttime-' + i + "']"
                        onePlayTime["from"] = $("#modifyTimeForm").find(startTimeName).val()
                        var endTimeName = "[name='" + 'endtime-' + i + "']"
                        onePlayTime["to"] = $("#modifyTimeForm").find(endTimeName).val()
                        var playcount = "[name='" + 'playcount-' + i + "']"
                        onePlayTime["playcount"] = parseInt($("#modifyTimeForm").find(playcount).val())
                        newPeriods.push(onePlayTime)
                    }
                    var postData = data;
                    postData.play_periods = newPeriods;
                    server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                        if(resp.status==0){
                            var newdata={}
                            newdata[event] = postData.play_periods;
                            obj.update(newdata);
                            layer.msg("更改播放时间段成功!",{icon:1})
                            layer.close(timeWin)
                        }else{
                            layer.msg("更改播放时间段失败!",{icon:2})
                        }
                    });
                })
            }else if(event=="content"){

                var taskType = data["task_type"]
                if(taskType == 1){ //文本类型
                    var contentWin = popWindow("修改任务文本内容!", "#modifyTextContent", ["80%", "60%"]);
                    $("#modifyTextForm").find('[name="textTitle"]').val(data.content[0]);
                    $("#modifyTextForm").find('[name="textContent"]').val(data.content[1]);

                    $("#modifyTextContent").find('[id="submit-textcontent"]').off("click")
                    //$("#submit-textcontent").off("click");
                    $("#modifyTextContent").find('[id="submit-textcontent"]').on("click",function (object) {
                        //alert(JSON.stringify(data));
                        var postData = data;
                        var content = data.content;
                        content[0] =   $("#modifyTextContent").find("input").val()
                        content[1] =   $("#modifyTextContent").find("textarea").val()

                        if(content[0].length==0){
                            layer.msg("标题必填!",{icon:0})
                            return
                        }else if(content[1].length==0){
                            layer.msg("内容必填!",{icon:0})
                            return;
                        }else{
                            if(data.ok_terminals.length > 0){
                                layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                                return
                            }else{
                                postData.content = content;
                                server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                                    if(resp.status==0){
                                        var newdata ={}
                                        newdata.content = content;
                                        obj.update(newdata);
                                        layer.msg("修改任务文本内容成功!",{icon:1})
                                        layer.close(contentWin)
                                    }else{
                                        layer.msg("修改任务文本内容失败!",{icon:2})
                                    }
                                });
                            }

                        }
                    })
                    $("#clearTextContent").off("click");
                    $("#clearTextContent").on("click",function (object) {
                        if(data.ok_terminals.length > 0){
                            layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                            return
                        }else{
                            $("#modifyTextContent").find("input").val("")
                            $("#modifyTextContent").find("textarea").val("")
                            form.render()
                        }

                    })
                }else {
                    initTransfer(obj)
                }

            }else if(event=="ready_terminals"){
                layer.msg("不可单独设置,请点击一键启用!",{icon:0})
            }else if(event=="ok_terminals"){
                var oldTerminals = data.ok_terminals
                var treeDataList = []
                if (AllTerminalList != null) {
                    var allTerminalList = AllTerminalList;
                    var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
                    if (TerminalGrpList != null) {
                        var groupList = TerminalGrpList;
                        treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,false)
                        tree.render({
                            id: 'terminalGrpTree'
                            , elem: '#terminalGrpTree'
                            , data: treeDataList
                            , isJump: true  //link 为参数匹配
                            , showCheckbox: true
                        });

                        $("#terminalGrpTree").append($('  <div  class="align-center verical-margin">\n' +
                            '        <div id="deleteTaskFromTerminals" class="layui-btn layui-btn-danger">在勾选的终端上删除此任务</div>\n' +
                            '</div>'))
                        $("#deleteTaskFromTerminals").off("click");
                        $("#deleteTaskFromTerminals").on("click", function (object) {
                            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                            var terminalIdList = common_api.getCheckTermialIds(checkedData)
                            if (terminalIdList.length > 0) {
                                var postData = {
                                    task_id:data.task_id,
                                    terminal_ids:terminalIdList
                                }
                                var loadingFlag = layer.msg('正在停止任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                                server_api.stopTask(JSON.stringify(postData), function (resp) {
                                    layer.close(loadingFlag)
                                    if (resp.status == 0) {
                                        var failedCnt = resp.result[0].failedCnt
                                        if(failedCnt >0){
                                            layer.msg("停止失败终端数:"+failedCnt, {icon: 0})
                                        }else{
                                            layer.msg("停止成功!", {icon: 1})
                                        }
                                        var tmpPostData ={task_id:data.task_id,plan_type:planType,plan_id:data.plan_id}
                                        server_api.getTaskByTaskId(JSON.stringify(tmpPostData),function (resp) {
                                            if(resp.status==0){
                                                var updateData = {}
                                                updateData.fail_terminals = resp.result[0]["fail_terminals"]
                                                updateData.ok_terminals = resp.result[0]["ok_terminals"]
                                                updateData.ready_terminals = resp.result[0]["ready_terminals"]
                                                obj.update(updateData)
                                                table.reload("taskList")
                                            }
                                        })
                                        layer.close(tmpIndex)
                                    } else {
                                        layer.msg(resp.msg, {icon: 2})
                                    }
                                })
                            } else {
                                layer.msg("未勾选任何终端!", {icon: 0})
                            }
                        })

                    }else{
                        getTerminalAndGroup()
                        layer.msg("没有获取到终端分组列表!",{icon:2})
                    }
                }else{
                    layer.msg("没有获取到终端列表!",{icon:2})
                    getTerminalAndGroup()
                }

            }else if(event=="fail_terminals"){
                var oldTerminals = data.fail_terminals
                var treeDataList = []
                if (AllTerminalList != null) {
                    var allTerminalList = AllTerminalList;
                    var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
                    if (TerminalGrpList != null) {
                        var groupList = TerminalGrpList;
                        treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,false)
                        tree.render({
                            id: 'terminalGrpTree'
                            , elem: '#terminalGrpTree'
                            , data: treeDataList
                            , isJump: true  //link 为参数匹配
                            , showCheckbox: true
                        });

                        $("#terminalGrpTree").append($('<div  class="align-center verical-margin">\n' +
                            '        <div id="deleteFailTerminal" class="layui-btn">删除失败记录</div>\n' +
                            '        <div id="resend2Terminal" class="layui-btn">重新下发此任务</div>\n' +
                            '    </div'))
                        $("#deleteFailTerminal").off("click");
                        $("#deleteFailTerminal").on("click", function (object) {
                            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                            var terminalIdList = common_api.getCheckTermialIds(checkedData)
                            if (terminalIdList.length > 0) {
                                var postData = {
                                    task_id:data.task_id,
                                    fail_terminals:terminalIdList
                                }
                                server_api.setTerminalsOfTask(JSON.stringify(postData), function (resp) {
                                    if (resp.status == 0) {
                                        layer.msg("删除成功!", {icon: 1})
                                     　　setCurrentData()

                                        layer.close(tmpIndex)
                                    } else {
                                        layer.msg(resp.msg, {icon: 2})
                                    }
                                })
                            } else {
                                layer.msg("未勾选任何终端!", {icon: 0})
                            }
                        })


                        $("#resend2Terminal").off("click");
                        $("#resend2Terminal").on("click", function (object) {

                            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                            var terminalIdList = common_api.getCheckTermialIds(checkedData)

                            if (terminalIdList.length > 0) {
                                var postData={
                                    task_id:data.task_id,
                                    terminal_type:"fail"
                                }
                                var loadingFlag = layer.msg('正在重发任务，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});
                                server_api.sendTask2terminal(JSON.stringify(postData),function (resp) {
                                    layer.close(loadingFlag)
                                    if(resp.status==0){
                                        var failedCnt = resp.result.failedCnt
                                        if(failedCnt >0){
                                            layer.msg("重新下发失败终端数:"+failedCnt, {icon: 0})
                                        }else{
                                            layer.msg("重新下发任务成功!", {icon: 1})
                                        }
                                        setCurrentData()
                                    }else{
                                        layer.msg(resp.msg, {icon: 2})
                                    }
                                })
                            } else {
                                layer.msg("未勾选任何终端!", {icon: 0})
                            }
                            //console.log(checkedData);
                        })
                    }else{
                        getTerminalAndGroup()
                        layer.msg("没有获取到终端分组列表!",{icon:2})
                    }
                }else{
                    layer.msg("没有获取到终端列表!",{icon:2})
                    getTerminalAndGroup()
                }
            }
        }
    );

    function getTerminalAndGroup() {
        var postData = {
            page: 1,
            pagesize: 10000,
            getTotal: "yes"
        }

        server_api.getAllTerminalOfUser(JSON.stringify(postData),function (terResp) {

            if (terResp.status == 0) {
                AllTerminalList = terResp.result;
                server_api.getTerminalGrp(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        TerminalGrpList = resp.result;
                    }else{
                        layer.msg("获取终端分组,任务无法下发!",{icon:2})
                    }
                })
            }
        })

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

    function getExamTemplateList(){
        var postData={}

        server_api.getExamTpllist(JSON.stringify(postData),function (resp) {
            console.log("getExamTpllist:",resp)
            if(resp.status==0){
                var colsList =[]
                var dataList = resp.result

                colsList.push({type: "numbers",title: '序号',width: 80,})
                colsList.push({title:'操作',align: 'center', width:180,templet:function (val) {
                        var createBtn = '<div class="layui-btn  layui-btn-xs layui-btn" lay-event="createExam">用此模板生成新的考试</div>'
                        return createBtn
                    }})

                colsList.push({
                    field: 'exam_name',
                    align: 'center',
                    title: '考试模板名称',
                    width: 300,
                })
                colsList.push({
                    field: 'start_date',
                    align: 'center',
                    title: '开始日期',
                    width: 120,
                })
                colsList.push({
                    field: 'end_date',
                    align: 'center',
                    title: '结束日期',
                    width: 120,
                })

                colsList.push({
                    field: 'am_start_time',
                    align: 'center',
                    title: '上午开始时间',
                    width: 120,
                })

                colsList.push({
                    field: 'am_end_time',
                    align: 'center',
                    title: '上午结束时间',
                    width: 120,
                })

                colsList.push({
                    field: 'pm_start_time',
                    align: 'center',
                    title: '下午开始时间',
                    width: 120,
                })

                colsList.push({
                    field: 'pm_end_time',
                    align: 'center',
                    title: '下午结束时间',
                    width: 120,
                })

                colsList.push({
                    field: 'count_down',
                    align: 'center',
                    title: '结束倒计时'
                })


                var renderParam = {
                    id: "taskTemplateList",//
                    elem: '#taskTemplateList',//指定表格元素
                    data: dataList,  //表格当前页面数据
                    toolbar: '#defaultTplToolBar',
                    defaultToolbar: ['filter'],
                    //cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                    skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
                    done: function (res, curr, count){

                    },
                    cols: [colsList]
                }
                table.render(renderParam);
            }else{
                layer.msg("获取默认模板列表失败!",{icon:0})
            }
        })
    }

    //监听默认考试信息模板修改
    table.on('tool(taskTemplateList)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if (event == "createExam") {
            layer.confirm('真的从此模板创建新的考试吗?', {icon: 3, title:'提示'}, function(index){
                var postData ={exam_id:data.exam_id}
                server_api.createExamFromTplId(JSON.stringify(postData),function (resp) {
                    console.log("creatExamFromTplId:",resp)
                    if(resp.status==0){
                        layer.msg("从模板创建考试成功！",{icon:1})
                        refreshExamList(resp.result[0].exam_id)
                    }else{
                        layer.msg("从模板创建考试失败！",{icon:2})
                    }
                })
            })
        }
    })


    function showCurrentExamInfo(theExam){
        var postData={
            exam_id:theExam.exam_id
        }
        server_api.getExamInfoById(JSON.stringify(postData),function (resp) {
            if(resp.status==0){
                var colsList =[]
                var dataList = resp.result
                currentExamInfo = dataList[0]
                $("#bodyContainer").find('[id="tableTitle"]').text("当前是考试<"　+ currentExamInfo.exam_name　+">的信息以及任务列表")
                $("#bodyContainer").find('[id="showExamInfo"]').show()

                colsList.push({
                    field: 'start_date',
                    align: 'center',
                    title: '开始日期',
                    width: 120,
                    event:"start_date"
                })
                colsList.push({
                    field: 'end_date',
                    align: 'center',
                    title: '结束日期',
                    width: 120,
                    event:"end_date"
                })

                colsList.push({
                    field: 'am_start_time',
                    align: 'center',
                    title: '上午开始时间',
                    width: 120,
                    event:"am_start_time"
                })

                colsList.push({
                    field: 'am_end_time',
                    align: 'center',
                    title: '上午结束时间',
                    width: 120,
                    event:"am_end_time"
                })

                colsList.push({
                    field: 'pm_start_time',
                    align: 'center',
                    title: '下午开始时间',
                    width: 120,
                    event:"pm_start_time"
                })

                colsList.push({
                    field: 'pm_end_time',
                    align: 'center',
                    title: '下午结束时间',
                    width: 120,
                    event:"pm_end_time"
                })

                colsList.push({
                    field: 'count_down',
                    align: 'center',
                    title: '结束倒计时(分钟,可编辑)',
                    width: 240,
                    edit:"text",
                })

                colsList.push({
                    field: 'timeRelate',
                    align: 'center',
                    title: '联动修改考试任务时间状态',
                    width: 240,
                    templet:function (val) {
                        var theSwitch = localStorage.getItem("timeRelate");
                        if(theSwitch!=null && theSwitch=="on"){
                            return  '<input type="checkbox" name="timeRelate" value="{{d.time_relate}}" lay-skin="switch" lay-text="开|关" lay-filter="timeRelate" checked>'
                        }else{
                            return  '<input type="checkbox" name="timeRelate" value="{{d.time_relate}}" lay-skin="switch" lay-text="开|关" lay-filter="timeRelate">'
                        }
                    },

                })
                colsList.push({
                    field: 'create_time',
                    align: 'center',
                    title: '创建时间',
                    templet:function (val) {
                        return val.create_time.substring(0,19)
                    }
                })


                var renderParam = {
                    id: "showExamInfo",//
                    elem: '#showExamInfo',//指定表格元素
                    data: dataList,  //表格当前页面数据
                    toolbar:"#examinfoToolbar",
                    defaultToolbar: [''],
                    //cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                    skin: 'line-row', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
                    done: function (res, curr, count){

                    },
                    cols: [colsList]
                }
                table.render(renderParam);
            }else{
                $("#bodyContainer").css("display","none")
                layer.msg("获取默认模板失败!",{icon:0})
            }
        })
    }


    table.on('tool(showExamInfo)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if (event == "start_date"||event=="end_date"){
            var renderOpt = {
                elem: this.firstChild
                , type: 'date'
                , show: true //直接显示
                , closeStop: this
                ,trigger: 'click'
                , done: function (value, date) {
                    var postData = {
                        exam_id:data.exam_id
                    }
                    postData[event] = value
                    var isValideDate = common_api.diffWithToday(value)
                    if(isValideDate >0){
                        server_api.examAddOrUpdate(JSON.stringify(postData),function (resp) {
                            if(resp.status!=0){
                                layer.msg("设置考试日期失败!",{icon:2})
                            }else{
                                var newdata ={}
                                newdata[event] = value;
                                obj.update(newdata);
                                layer.msg("设置成功!",{icon:1})
                            }
                        })
                    }else{
                        layer.msg("考试日期不能早于今天！",{icon:0})
                        var newdata ={}
                        newdata[event] = data[event];
                        obj.update(newdata);
                    }
                }
            }


            laydate.render(renderOpt);
        }else if(event=="am_start_time"||event=="am_end_time"||event=="pm_start_time"||event=="pm_end_time"){
            var renderOpt = {
                elem: this.firstChild
                , type: 'time'
                ,format:"HH:mm"
                , show: true //直接显示
                , closeStop: this
                ,trigger: 'click'
                , done: function (value, date) {

                    if(event=="am_start_time"||event=="am_end_time"){
                        var check = common_api.timeCalculateMinute(value,"12:00")
                        if(check >0){
                            layer.msg("无效的上午时间!",{icon:0})
                            return
                        }
                    }else if(event=="pm_start_time"||event=="pm_end_time"){
                        var check = common_api.timeCalculateMinute(value,"12:00")
                        if(check <0){
                            layer.msg("无效的下午时间!",{icon:0})
                            return
                        }
                    }



                    var postData = {
                        exam_id:data.exam_id
                    }
                    postData[event] = value
                    server_api.examAddOrUpdate(JSON.stringify(postData),function (resp) {
                        if(resp.status!=0){
                            layer.msg("设置考试日期失败!",{icon:2})
                        }else{
                            var newdata ={}
                            newdata[event] = value;
                            var isOn = localStorage.getItem("timeRelate");
                            if("on"==isOn && value!=null && value.length >0){
                                var distance = common_api.timeCalculateMinute(value,data[event])
                                if(distance!=0){
                                    if(event=="am_start_time" && newdata["am_end_time"]){
                                        newdata["am_end_time"] = common_api.timeAddValue(data["am_end_time"],distance)
                                    }else if(event=="am_end_time" && newdata["am_start_time"]){
                                        newdata["am_start_time"] = common_api.timeAddValue(data["am_start_time"],distance)
                                    }

                                    if(event=="pm_start_time" &&  newdata["pm_end_time"]){
                                        newdata["pm_end_time"] = common_api.timeAddValue(data["pm_end_time"],distance)
                                    }else if(event=="pm_end_time" && newdata["pm_start_time"]){
                                        newdata["pm_start_time"] = common_api.timeAddValue(data["pm_start_time"],distance)
                                    }
                                }
                            }

                            obj.update(newdata)
                            var n =0
                            if(event=="am_start_time"){
                                n=0
                                updateAllTaskPeroid(value,n)
                            }else if(event=="pm_start_time"){
                                n=1
                                updateAllTaskPeroid(value,n)
                            }
                            layer.msg("设置成功!",{icon:1})
                        }
                    })
                }
            }

            if(event=="am_start_time"||event=="am_end_time"){
                renderOpt.min = '00:00:00'
                renderOpt.max = '12:00:00'
            }else {
                renderOpt.min = '12:00:00'
                renderOpt.max = '23:59:00'
            }

            laydate.render(renderOpt);
        }
    })

    //监听单元格编辑
    table.on('edit(showExamInfo)', function(obj){
        var value = obj.value //得到修改后的值
            ,data = obj.data //得到所在行所有键值
            ,field = obj.field; //得到字段
        var data = obj.data;
        var postData = data;
        if(field=="count_down"){
            postData.exam_id = data.exam_id
            postData["count_down"] = value;
            server_api.examAddOrUpdate(JSON.stringify(postData), function (resp) {
                if(resp.status==0){
                    var newdata = {};
                    newdata["count_down"] = value;
                    obj.update(newdata);
                    layer.msg("修改考试倒计时成功!",{icon:1})
                }else{
                    layer.msg("修改考试倒计时成功!",{icon:2})
                }
            });
        }
    });

    form.on('switch(timeRelate)', function(obj){
        if(obj.elem.checked){
            localStorage.setItem("timeRelate","on");
            layer.msg("开启后设置考试时间将会联动设置任务的执行时间！",{icon:0})
        }else{
            localStorage.setItem("timeRelate","off");
        }
    });

    function showExamInfo(theExam) {

        if(theExam){
            currentPlanId = theExam.exam_id
            $("#examTitleInfo").show()
            $("#examDetail").show()
            showCurrentExamInfo(theExam)
            setCurrentData("yes");
        }else{
            $("#bodyContainer").find('[id="tableTitle"]').text("")
            $("#examDetail").hide()
        }
    }

    function refreshExamList(showExamId){
        var postData = {
            page:1,
            pagesize:9999
        }
        server_api.getExamList(JSON.stringify(postData),function (resp) {
            if(resp.status==0){
                var examList = resp.result

                var treeDataList = []
                var allExams = {}
                allExams.title = "所有考试列表"
                allExams.id = 1
                allExams.children = []
                allExams.spread = true
                // allExams.edit = ['add', 'update', 'del']
                treeDataList.push(allExams)

                for(var i=0;i<examList.length;i++){
                    var oneExam = {}
                    oneExam.title = examList[i].exam_name
                    oneExam.exam_name = examList[i].exam_name
                    oneExam.exam_code = examList[i].exam_code
                    oneExam.exam_id = examList[i].exam_id
                    oneExam.exam_date = examList[i].exam_date
                    oneExam.start_time = examList[i].start_time
                    oneExam.end_time = examList[i].end_time
                    oneExam.count_down = examList[i].count_down
                    oneExam.id = i+2
                    allExams.children.push(oneExam)
                    if(showExamId){
                        showExamInfo({exam_id:showExamId})
                    }else if(i==examList.length-1 && currentPlanId==null){
                        showExamInfo(oneExam)
                    }
                }

                tree.render({
                    id: 'examList'
                    ,elem: '#examList'
                    ,data: treeDataList
                    ,edit: ['update']
                    ,showCheckbox: true
                    ,click: function(obj){
                        // layer.alert(JSON.stringify(obj.data));
                        var postData={
                            exam_id:obj.data.exam_id
                        }
                        server_api.getExamInfoById(JSON.stringify(postData),function (resp) {
                            if(resp.status==0){
                                var tmpExam = resp.result[0]
                                currentPlanId = obj.data.exam_id
                                showExamInfo(tmpExam)

                            }
                        })
                    }
                    ,operate: function(obj){
                        var type = obj.type; //得到操作类型：add、edit、del
                        var data = obj.data; //得到当前节点的数据
                        var elem = obj.elem; //得到当前节点元素
                        var id = data.id; //得到节点索引
                        if(type === 'update'){ //修改节点
                            var postData ={
                                exam_id:obj.data.exam_id,
                                exam_name:obj.data.title,
                            }
                            //layer.alert(JSON.stringify(postData))
                            server_api.examAddOrUpdate(JSON.stringify(postData),function (resp) {
                                if(resp.status==0){
                                    layer.msg("修改考试信息成功!",{icon:1})
                                    server_api.getExamInfoById(JSON.stringify(postData),function (resp) {
                                        if(resp.status==0){
                                            var tmpExam = resp.result[0]
                                            tmpExam.exam_id = obj.data.exam_id
                                            showExamInfo(tmpExam)
                                        }
                                    })
                                }else{
                                    layer.msg("修改考试信息失败!",{icon:2})
                                }
                            })
                        }
                    }
                });
            }else{
                tree.render({
                    id: 'examList'
                    , elem: '#examList'
                    , data: []
                })
                showExamInfo(null)
                layer.msg(resp.msg,{icon:2})
            }
        })
    }
    $(function () {
        getTerminalAndGroup()
        getExamTemplateList()


        var postData ={
            page:1,
            pagesize:10000
        }

        $("#title-1").text("首页")
        $("#title-2").text("任务管理")
        $("#title-3").text("考试预案")

        $("#addExamInfo").on("click",function (object) {
            var newExamWin = popWindow("新增考试","#addExam", ['50%', '35%'])

            form.on("submit(submitNewExam)",function (obj) {
                var postData = obj.field
                postData["start_date"] = common_api.getToDayDate()
                postData["end_date"] = common_api.getToDayDate()
                postData["am_start_time"] = "09:00"
                postData["am_end_time"] = "11:00"
                postData["pm_start_time"] = "14:00"
                postData["pm_end_time"] = "16:00"
                server_api.examAddOrUpdate(JSON.stringify(postData),function (resp) {
                    layer.close(newExamWin)
                    if(resp.status==0){
                        layer.msg("新增考试成功,请设置考试日期和时间!",{icon:1})
                        refreshExamList(resp.exam_id)
                    }else{
                        layer.msg("新增考试失败!",{icon:2})
                    }

                })

                return false
            })
        })

        $("#delExamInfo").on("click",function (object) {
            var checkExams = tree.getChecked('examList');
            if(checkExams==null ||checkExams.length==0){
                layer.msg("未勾选需要删除的考试",{icon:0})
            }else{
                layer.confirm('删除勾选的考试将会一并删除其关联的任务,操作不可恢复!', {icon: 3, title:'提示'}, function(index){
                    var examList = checkExams[0].children
                    var examIds =[]
                    var isCurrentDel = false
                    for(var i=0;i<examList.length;i++){
                        examIds.push(examList[i].exam_id)
                        if(currentPlanId ==examList[i].exam_id){
                            isCurrentDel=true
                        }
                    }

                    var postData ={
                        exam_ids:examIds
                    }

                    server_api.delExamInfoAndTask(JSON.stringify(postData),function (resp) {
                        if(resp.status==0){
                            layer.msg("删除考试及其相关任务成功!",{icon:1})
                            if(isCurrentDel){
                                currentPlanId = null
                            }
                            refreshExamList()
                        }else{
                            layer.msg("删除考试失败!",{icon:2})
                        }
                    })
                })

            }
        })

        refreshExamList()

    })

});