layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});
var weeks = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
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
    5: "最高级",
    6: "临时任务",
    999:"紧急任务"
}
var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}


layui.use(['jquery', 'form', 'tree', 'table', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        table = layui.table,
        laypage = layui.laypage,
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

    function setCurrentData(getTotal,terminalId, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            action:"normal_plan",
            getTotal: getTotal
        }

        if(terminalId && terminalId.length >0){
            postData.terminal_id = terminalId
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getTerminalLog(JSON.stringify(postData), function (resp) {
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
            renderDataList(parseDataList(dataList));
        });
    }


    function parseDataList(dataList) {
        var restDataList =[]
        for(var i=0;i<dataList.length;i++){
            var taskInfo = JSON.parse(dataList[i].content)
            var newDataItem ={}
            newDataItem.id = dataList[i].id
            newDataItem.task_name = taskInfo.taskName
            newDataItem.task_status = function (start_date,end_date,result) {
                var starttime = new Date(Date.parse(start_date));
                var endtime = new Date(Date.parse(end_date));
                if(result=="terminal_del"){
                    return '<span class="layui-btn layui-btn-xs layui-btn-danger">终端已清除</span>'
                }if(starttime > new Date()){
                    return '<span class="layui-btn layui-btn-xs layui-btn-normal">未开始</span>'
                }else if(endtime < new Date()){
                    return '<span class="layui-btn layui-btn-xs layui-btn-disabled">已经结束</span>'
                }else{
                    return '<span class="layui-btn layui-btn-xs layui-btn-checked">处于执行期间</span>'
                }
            }(taskInfo.playDate.from,taskInfo.playDate.to,dataList[i].result)
            newDataItem.create_time = dataList[i].create_time
            newDataItem.task_type =taskTypeMap[taskInfo.taskType]
            newDataItem.task_set = function (theTask) {
                var showSrt =""
                showSrt +="优先级:" + priorityMap[theTask.level] + "<br>"
                showSrt +="循环方式:" + playModeMap[theTask.playDate.mode] + "<br>"
                showSrt +="开始日期:" + taskInfo.playDate.from +",结束日期:" + taskInfo.playDate.to + "<br>"
                return showSrt
            }(taskInfo)

            restDataList.push(newDataItem)
        }

        return restDataList

    }



    function renderDataList(dataList) {
        table.render({
            id: "taskHistoryList",//
            elem: '#taskHistoryList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            toolbar: '#toolbarAction',
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
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
                {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
                {field: 'task_name', align: 'left', title: '任务名字', width: 300, sort: true},
                {field: 'task_status', align: 'left', title: '任务状态', width:120, sort: true},
                {field: 'task_type', align: 'left', title: '任务类型', width: 140, sort: true},
                {field: 'task_set', align: 'left', title: '任务设置', width: 400},
                {field: 'create_time', align: 'left', title: '下发时间', cellMinWidth: 140, sort: true}


            ]]
        })
    }


    function getTerminalGrpList() {
        var postData = {
            page: 1,
            pagesize: 10000,
            getTotal: "yes"
        }

        var treeDataList = []
        var itemtId = 0
        server_api.getTerminalList(JSON.stringify(postData),function (terResp) {
            if (terResp.status == 0) {
                var allTerminalList = terResp.result;
                var tmpMap = {}

                server_api.getTerminalGrp(JSON.stringify(postData),function (resp) {
                    if (resp.status == 0) {
                        var groupList = resp.result;
                        var oneGroup1 = {}
                        oneGroup1.title = "未分组终端"
                        oneGroup1.id = itemtId++
                        oneGroup1.gid = "ungroup"
                        oneGroup1.disabled = true
                        oneGroup1.nTerminal = 0
                        oneGroup1.children = []
                        tmpMap["ungroup"] = 0
                        treeDataList.push(oneGroup1)
                        for (var i = 0; i < groupList.length; i++) {
                            var oneGroup = {}
                            oneGroup.title = groupList[i].group_name
                            oneGroup.id = itemtId++
                            oneGroup.gid = groupList[i].gid
                            oneGroup.disabled = true
                            oneGroup.nTerminal = 0
                            oneGroup.children = []
                            tmpMap[groupList[i].gid] = i + 1
                            treeDataList.push(oneGroup)
                        }
                        for (var m = 0; m < allTerminalList.length; m++) {
                            if (allTerminalList[m].state == 0) {
                                continue
                            } else {
                                var oneTerminal = {}
                                oneTerminal.title = allTerminalList[m].name + '(' + allTerminalList[m].ip + ')'
                                oneTerminal.id = itemtId++
                                oneTerminal.terminal_id = allTerminalList[m].terminal_id
                                if (allTerminalList[m].gids && allTerminalList[m].gids.length > 0) {
                                    for (var w = 0; w < allTerminalList[m].gids.length; w++) {
                                        treeDataList[tmpMap[allTerminalList[m].gids[w]]].children.push(oneTerminal)
                                        treeDataList[tmpMap[allTerminalList[m].gids[w]]].nTerminal++
                                        treeDataList[tmpMap[allTerminalList[m].gids[w]]].disabled = false
                                    }
                                } else {
                                    treeDataList[0].children.push(oneTerminal)
                                    treeDataList[0].nTerminal++
                                    treeDataList[0].disabled = false
                                }
                            }
                        }
                        tree.render({
                            id: 'terminalGrpTree'
                            , elem: '#terminalGrpTree'
                            , data: treeDataList
                            , isJump: true  //link 为参数匹配
                            ,showLine: false  //是否开启连接线
                            // , showCheckbox: true
                            ,click: function(node){
                                if(node.data.gid && node.data.gid.length >0){
                                    return
                                }else{
                                    var terminal_id = node.data.terminal_id
                                    $("#tableTitle").text("当前查看的是 " + node.data.title + " 的历史任务")
                                    $("#allTerminals").attr("checked",false)
                                    form.render()
                                    setCurrentData("yes",terminal_id)
                                }
                            }
                        });
                    }
                })
            }
        })

    }

    form.on('checkbox', function(obj){
        var inputId = $(obj.elem).attr('id')
        if(inputId =="allTerminals"){
            $("#tableTitle").text("当前查看的是全部终端的历史任务")
            setCurrentData("yes")
        }
    });


    $("#getTermialsTasks").on("click",function (object) {
        var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
        var terminal_ids ={}
        var terminalIdList=[]
        for(var h=0;h<checkedData.length;h++){
            if(checkedData[h].nTerminal == checkedData[h].children.length && checkedData[h].children.length>0){
                group_ids.push(checkedData[h].gid)
            }else if(checkedData[h].children.length>0){
                //var regex = /('(\w+)')/g;
                for(var i=0;i<checkedData[h].children.length;i++){
                    terminal_ids[checkedData[h].children[i].terminal_id] = 1
                    // terminal_ids.push(checkedData[h].children[i].terminal_id)
                }
            }
        }
        for(var terminalid in terminal_ids){
            terminalIdList.push(terminalid)
        }
    })

    $(function () {
        getTerminalGrpList()
        setCurrentData("yes")
    })
})