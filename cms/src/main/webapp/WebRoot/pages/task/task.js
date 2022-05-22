layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api',
    common_api: 'common_api'
});
var planType ="normal"
var currentPlanId = null
var weeks = ["周一","周二","周三","周四","周五","周六","周日"]

var taskTypeMap = {
    1: "文本类型",
    2: "图片类型",
    3: "音频类型",
    4: "视频类型"
}

var fileTypeMap ={
    1: "图片类型",
    2: "音频类型",
    3: "视频类型",
    4:"apk文件"
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

var newDefaultTask ={
    plan_type:"normal",
    task_name:"新的测试任务（请修改此任务)",
    task_type:1,
    priority:1,//默认最低级
    start_date:"2019-09-03",
    end_date:"2019-09-03",
    play_mode:1,//每天循环
    week_days:[],
    play_periods:[{"from":"00:00","to":"00:00",playcount:0}],
    content:["请修改","请修改"]
}

var newTemporaryTask={
    plan_type:"temporary",
    task_name:"新的测试任务（请修改此任务)",
    task_type:1,
    priority:6,//默认超出普通任务一个等级
    start_date:"2019-09-03",
    end_date:"2019-09-03",
    play_mode:1,//每天循环
    week_days:[],
    play_periods:[{"from":"00:00","to":"23:59",playcount:0}],
    content:["请修改","请修改"]
}

var TerminalGrpList =null
var AllTerminalList =null



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
    form.render()
    //全局变量区域

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

    function buildSelect(itemMap, id, selected,isDisabled) {
        var selectElem = $('<select></select>')
        selectElem.attr("name", id)
        selectElem.attr("id", id)
        selectElem.attr("lay-ignore", "")
        selectElem.attr("class", "table-inner-select")

        if(isDisabled){
            selectElem.attr("disabled", isDisabled)
        }

        selectElem.css("height",20)

        for (var key in itemMap) {
            var optItem = $("<option></option>");
            optItem.attr("value", key);
            optItem.text(itemMap[key]);
            if (key == selected) {
                optItem.attr("selected", true);
            }
            selectElem.append(optItem);
        }

        //console.log("selectElem:",selectElem)

        return selectElem[0].outerHTML
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
            var showText = "启用"
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
                showBtn += '<span class="layui-btn layui-btn-xs layui-btn-normal" lay-event="ready_terminals">' + val.ready_terminals.length +'个未下发(点击下发)</span><br>';
                if(val.ok_terminals.length >0){
                    showBtn += '<span class="layui-btn layui-btn-xs layui-btn-checked" lay-event="ok_terminals">'+val.ok_terminals.length　+'个已下发(点击停用)</span><br>';
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
            // templet: function (val) {
            //     var disabled = false
            //     if(val.ok_terminals.length >0){
            //         disabled = true
            //     }
            //     console.log("run into build type select!!!")
            //     return buildSelect(taskTypeMap, "taskTypeChoice" + val.id, val.task_type,disabled);
            // }
        })

        colsList.push( {
            field: 'content',
            align: 'left',
            title: '任务内容',
            width:200,
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

        if(planType =="normal"){
            colsList.push({
                field: 'start_date',
                align: 'left',
                title: '开始日期',
                width: 120,
                event: "start_date"
            })

            colsList.push( {
                field: 'end_date',
                align: 'left',
                title: '结束日期',
                width: 120,
                event: "end_date"
            })
        }

        if(planType != "temporary"){ //临时任务没有这些列
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

            colsList.push({
                field: 'play_mode',
                align: 'left',
                title: '循环方式',
                width: 140,
                event: "play_mode",
                templet:"#selectTaskPlayMode"
                // templet: function (val) {
                //     var disabled = false
                //     if(val.ok_terminals.length >0){
                //         disabled = true
                //     }
                //     return buildSelect(playModeMap, "playModeChoice" + val.id, val.play_mode,disabled);
                // }
            })

            colsList.push({
                field: 'week_days',
                align: 'left',
                title: '每周播放日',
                width: 300,
                event: "week_days",
                templet: function (val) {
                    var showStr=""

                    if(val.play_mode ==2){
                        if(val.week_days && val.week_days.length >0){
                            for(var i=0;i<val.week_days.length;i++){
                                if(showStr.length==0){
                                    showStr +=weeks[val.week_days[i]-1]
                                }else{
                                    showStr +="，" + weeks[val.week_days[i]-1]
                                }
                            }
                            return showStr
                        }else{
                            return "未设置"
                        }

                        //return buildCheck(val.id,val.week_days)
                    }else{
                        return "没有设置按周循环"
                    }
                    //return buildSelect(playModeMap, "playModeChoice" + val.id, val.play_mode);
                }
            })

            colsList.push( {
                field: 'priority',
                align: 'left',
                title: '优先级',
                width: 120,
                event: "priority",
                templet:"#selectTaskPriority",
                // templet: function (val) {
                //     var disabled = false
                //     if(val.ok_terminals.length >0){
                //         disabled = true
                //     }
                //     return buildSelect(priorityMap, "priorityChoice" + + val.id, val.priority,disabled);
                // }
            })
        }
        // else{
        //     //临时任务包含播放次数和结束时间
        //     colsList.push({
        //         field: 'play_periods',
        //         // colspan: 3,
        //         align: 'center',
        //         title: '执行结束时间',
        //         width: 340,
        //         event: "play_periods",
        //         templet: function (val) {
        //             var showStr = "结束:" + val.play_periods[0].to;
        //                 if (val.play_periods[i].playcount == 0) {
        //                     showStr += ", 播放次数:循环";
        //                 } else {
        //                     showStr += ", 播放次数:" + val.play_periods[i].playcount;
        //                 }
        //                 showStr += "<br>";
        //             return showStr;
        //         }
        //     })
        // }

        colsList.push( {field: 'create_user', align: 'center', title: '创建人',width:140})
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
                if(planType=="temporary"){
                    $("#addTaskBtn").html("<i class=\"fa fa-plus icon-margin\"></i>新增临时任务")
                }else{
                    form.on('checkbox(liandong_time)', function(checkObj){
                        var isLiandong = $("#playtime_liandong").prop("checked");
                        if(isLiandong){
                            layer.msg("设置考试时间将会联动调整任务执行时间!",{icon:0})
                        }
                    });
                }

                // $(".layui-table-main  tr").each(function (index ,val) {
                //     $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                // });
                //form.render();
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

    function updateAllTaskPeroid(newTime) {
        var isLiandong = $("#playtime_liandong").prop("checked");
        if(!isLiandong){
            return
        }
        var allTask = common_api.simpleCopyObject(table.cache["taskList"])
        if(allTask!=null){
            var beginTime = allTask[0].play_periods[0].from
            var distance = common_api.timeCalculateMinute(newTime,beginTime)
            for(var i=0;i<allTask.length;i++){
                var playPeriods = common_api.simpleCopyObject(allTask[i].play_periods)
                for(var n=0;n<playPeriods.length;n++){
                    if(i==0 && n==0){
                        playPeriods[n].from = newTime
                        playPeriods[n].to = common_api.timeAddValue(playPeriods[n].to,distance)
                    }else{
                        playPeriods[n].from = common_api.timeAddValue(playPeriods[n].from,distance)
                        playPeriods[n].to = common_api.timeAddValue(playPeriods[n].to,distance)
                    }
                }
                //allTask[i].play_periods = playPeriods
                updateTaskInfo(allTask[i].task_id,"play_periods",playPeriods)
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
                                    // var recodePage = $(".layui-laypage-skip .layui-input").val();
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
                layer.msg(resp.msg, {icon: 2});
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
                    if(planType=="normal"){
                        tmpTask = newDefaultTask
                        tmpTask.plan_type = planType
                    }else if(planType = "temporary"){
                        tmpTask =  newTemporaryTask
                        tmpTask.plan_type = planType
                    }
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
                                if(requestCnt==task_ids.length){
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

            } else if (event == "start_date" || event == "end_date") {
                // var field = data('field');
                if(data.ok_terminals.length > 0){
                    layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                    return
                }


                laydate.render({
                    elem: this.firstChild
                    , type: 'date'
                    , show: true //直接显示
                    , closeStop: this
                    , done: function (value, date) {
                        var newdata = {};
                        var isValideData = common_api.diffWithToday(value)
                        if(isValideData <0){
                            layer.msg("设置的日期不能早于今天！",{icon:0})
                            newdata[event] = data[event];
                            obj.update(newdata);
                            return
                        }
                        if (!value || event.length == 0) {
                            layer.msg("必须设置日期!");
                            newdata[event] = data[event];
                            obj.update(newdata);
                            return;
                        } else if (value != data[event]) {
                            var postData = data;
                            postData[event] = value;
                            server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                                if(resp.status==0){
                                    newdata[event] = value;
                                    obj.update(newdata);
                                    obj.update(newdata);
                                    updateTaskInfo(data.task_id,event,value)
                                    layer.msg("修改成功!",{icon:1})
                                }else{
                                    layer.msg("修改失败!原因:"+resp.msg,{icon:1})
                                }
                            });
                        }
                    }

                });
            } else if (event == "play_periods") {
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
            }else if(event=="week_days"){
                if(data.ok_terminals.length > 0){
                    layer.msg("任务存在启用的终端,不可以修改!",{icon:0})
                    return
                }
                if(data.play_mode != 2){
                    layer.msg("请先设置循环方式为[按周循环]!",{icon:0})
                    return
                }
                $("#weekChoiceList").empty()
                var weekChoiceList = $("#weekCheckForm").find("[id=weekChoiceList]")
                for(var i=0;i<weeks.length;i++){
                    var newInput = $("<input>")
                    newInput.attr("type","checkbox")
                    newInput.attr("name","week-"+ data.id + "-" + i)
                    newInput.attr("title",weeks[i])
                    for(var j=0;j<data.week_days.length;j++){
                        if(data.week_days[j] == i +1){
                            newInput.attr("checked",true)
                            break
                        }
                    }
                    weekChoiceList.append(newInput)
                }
                form.render();
                var tmpWinIndex =  popWindow("请选择每周播放日","#weekCheck",["800px", "150px"])
                $("#submit-week").off("click");
                $("#submit-week").on("click",function (submitWeekDays) {
                    var week_days =[]
                    var checkedWeekDay = $("#weekCheckForm").find("[name^=week-]:checked")
                    if(checkedWeekDay.length==0){
                        layer.msg("至少选择一个！",{icon:0})
                        return
                    }
                    for(var i=0;i<checkedWeekDay.length;i++){
                        var tmpId = $(checkedWeekDay[i])[0].name.split("-")[2]
                        week_days.push(parseInt(tmpId) + 1)
                    }

                    var postData = data;
                    postData.week_days = week_days;
                    server_api.addOrUpdateTask(JSON.stringify(postData), function (resp) {
                        if(resp.status==0){
                            var newdata ={}
                            newdata.week_days = week_days;
                            obj.update(newdata);
                            table.reload("taskList")
                            layer.msg("设置成功!",{icon:1})
                        }else{
                            layer.msg(resp.msg,{icon:2})
                        }
                        layer.close(tmpWinIndex)
                    });
                })


            } else if(event=="content"){

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

                var oldTerminals = data.ready_terminals
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
                            '        <div id="saveReadTerminals" class="layui-btn">保存配置</div>\n' +
                            '        <div id="saveTerminalAndSend" class="layui-btn">立即下发</div>\n' +
                            '    </div>'))
                        $("#saveReadTerminals").off("click");
                        $("#saveReadTerminals").on("click", function (object) {
                            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                            var terminalIdList = common_api.getCheckTermialIds(checkedData)
                            if (terminalIdList.length > 0) {
                                var postData = {
                                    task_id:data.task_id,
                                    ready_terminals:terminalIdList
                                }
                                server_api.setTerminalsOfTask(JSON.stringify(postData), function (resp) {
                                    if (resp.status == 0) {
                                        layer.msg("保存配置成功!", {icon: 1})
                                        //layer.close(winIndex)
                                        var newdata = {}
                                        newdata.ready_terminals = terminalIdList;
                                        obj.update(newdata);
                                        layer.close(tmpIndex)
                                    } else {
                                        layer.msg(resp.msg, {icon: 2})
                                    }
                                })
                            } else {
                                layer.msg("未勾选任何终端!", {icon: 0})
                            }
                        })

                        $("#saveTerminalAndSend").off("click");
                        $("#saveTerminalAndSend").on("click", function (object) {

                            var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                            var terminalIdList = common_api.getCheckTermialIds(checkedData)

                            if (terminalIdList.length > 0) {
                                var isPass = common_api.diffWithToday(data.end_date)
                                if(isPass <0){
                                    layer.msg("任务日期已经过期！",{icon:0})
                                    return
                                }


                                var postData2={
                                    task_id:data.task_id,
                                    terminal_ids:terminalIdList,
                                    terminal_type:"ready"
                                }
                                var loadingFlag = layer.msg('正在下发，请稍候……', {icon: 16, shade: 0.01, shadeClose: false, time: 0});

                                server_api.sendTask2terminal(JSON.stringify(postData2),function (resp) {
                                    layer.close(loadingFlag)
                                    if(resp.status==0){
                                        var failedCnt = resp.result[0].failedCnt
                                        if(failedCnt >0){
                                            layer.msg("发送失败终端数:"+failedCnt, {icon: 0})
                                        }else{
                                            layer.msg("发送成功!", {icon: 1})
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
                                            layer.close(tmpIndex)
                                        })
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
                        layer.msg("获取终端分组失败,任务无法下发!",{icon:2})
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


    $(function () {
        var tmpplanType = GetQueryString("plan_type")
        getTerminalAndGroup()
        if(tmpplanType=="temporary"){
            $("#title-1").text("首页")
            $("#title-2").text("任务管理")
            $("#title-3").text("临时任务")
            planType = "temporary"
            setCurrentData("yes");
        }else{
            $("#title-1").text("首页")
            $("#title-2").text("任务管理")
            $("#title-3").text("定时任务")
            setCurrentData("yes");
        }

    })

});