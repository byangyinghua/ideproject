layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});


var fileTypeMap = {
    "2": "音频类型",
    "3": "视频类型"
}

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var TerminalGrpList =null
var AllTerminalList =null
layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api','common_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        common_api = layui.common_api,
        server_api = layui.server_api;

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


        return selectElem[0].outerHTML
    }


    function RefreshPlayingList() {

        server_api.GetPlayinglist(null,function (resp) {
            if(resp.status==0){
                var dataList = resp.result
                table.render({
                    id: "thePlayingList",//
                    elem: '#thePlayingList',//指定表格元素
                    data: dataList,  //表格当前页面数据
                    limit: 999,
                    toolbar: '#toolbarPlaying',
                    defaultToolbar: ['filter'],
                    cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
                    skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
                    done: function (res, curr, count) {
                        $(".layui-table-main  tr").each(function (index, val) {
                            $($(".layui-table-fixed .layui-table-body tbody tr")[index]).height($(val).height());
                        });
                    },
                    cols: [[
                        {type:"numbers"},
                        {field: 'start_time', align: 'left', title: '开始推流时间', width: 160},
                        {field: 'state', align: 'left', title: '状态(点击可播放)', width: 160,templet:function (val) {
                                if(val.state=="0"||val.state==0){
                                    return '<span class="layui-btn layui-btn-xs layui-btn-primary">未启动</span>';
                                }else if(val.state=="1"||val.state==1){
                                    return '<span class="layui-btn layui-btn-xs" style="background-color: #FFB800;">等待推流中</span>';
                                }else if(val.state=="2"||val.state==2){
                                    return '<span class="layui-btn layui-btn-xs layui-btn-checked">推流播放中</span>';
                                }else if(val.state=="3"||val.state==3){
                                    return '<span class="layui-btn layui-btn-xs layui-btn-danger">推流结束</span>';
                                }else{
                                    return "未知状态"
                                }
                            }},
                        {field: 'name', align: 'left', title: '预案名字', width: 240},
                        {field: 'attach_type', align: 'left', title: '流类型', width: 100,templet:function (val) {
                                var disabled = false
                                if(val.state =="1"||val.state =="2"){
                                    disabled = true
                                }
                                return buildSelect(fileTypeMap, "mediaTypeChoice" + val.id, val.attach_type,disabled);
                            }},

                        {field: 'attach_name', align: 'left', title: '媒体文件名字', width: 300,templet: function (val) {
                                if (!val.attach_name || val.attach_name == "") {
                                    return '<div><span class="layui-btn layui-btn-xs layui-btn-normal">请选择</span></div>';
                                } else {
                                    var tmpName = val.attach_name.split(".")[0]
                                    tmpName = tmpName.substring(0, 12);
                                    if (tmpName.length > 12) {
                                        title += "……"
                                    }
                                    return '<div>' + val.attach_name + '</div>';
                                }
                            }},
                        {field: 'playingCnt', align: 'center', title: '在播终端数', width: 100,templet:function (val) {
                                if(val.playingCnt==0){
                                    return '<span class="layui-btn layui-btn-xs layui-btn-danger">0</span>';
                                }else{
                                    return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + val.playingCnt + '</span>';
                                }
                            }},
                        {field: 'creator', align: 'center', title: '创建人'}

                    ]]
                })
            }
        })

    }

    table.on('toolbar(thePlayingList)',function (obj) {
        if(obj.event=="refreshPlayingList"){
            RefreshPlayingList()
        }
    })


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

    function setCurrentData(getTotal, isLast, isFirst) {
        var postData = {
            page: pageData.page,
            pagesize: pageData.pagesize,
            getTotal: getTotal
        }

        if (isLast && isLast == "yes") {
            postData.page = pageData.lastPage;
        } else if (isFirst && isFirst == "yes") {
            postData.page = 1;
        }

        server_api.getLivePlayList(JSON.stringify(postData), function (resp) {
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


    function renderDataList(dataList) {
        table.render({
            id: "liveplayList",//
            elem: '#liveplayList',//指定表格元素
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
                //{field: 'live_id', align: 'left', title: '预案编号', width: 260, sort: true},
                {title: '操作', toolbar: '#playActions', width: 120},
                {field: 'start_time', align: 'left', title: '开始推流时间', width: 160, event:"start_time"},
                {field: 'state', align: 'left', title: '状态(点击可播放)', width: 160,event:"state",templet:function (val) {
                        if(val.state=="0"||val.state==0){
                            return '<span class="layui-btn layui-btn-xs layui-btn-primary">未启动</span>';
                        }else if(val.state=="1"||val.state==1){
                            return '<span class="layui-btn layui-btn-xs" style="background-color: #FFB800;">等待推流中</span>';
                        }else if(val.state=="2"||val.state==2){
                            return '<span class="layui-btn layui-btn-xs layui-btn-checked">推流播放中</span>';
                        }else if(val.state=="3"||val.state==3){
                            return '<span class="layui-btn layui-btn-xs layui-btn-danger">推流结束</span>';
                        }else{
                            return "未知状态"
                        }
                    }},
                {field: 'name', align: 'left', title: '预案名字(可修改）', width: 240, sort: true,edit:'text'},
                {field: 'attach_type', align: 'left', title: '流类型', width: 100,event:"mediaTypeChoice",templet:function (val) {
                        var disabled = false
                        if(val.state =="1"||val.state =="2"){
                            disabled = true
                        }
                        return buildSelect(fileTypeMap, "mediaTypeChoice" + val.id, val.attach_type,disabled);
                    }},

                {field: 'attach_name', align: 'left', title: '媒体文件名字', width: 300,event:"attach_name",templet: function (val) {
                        if (!val.attach_name || val.attach_name == "") {
                            return '<div><span class="layui-btn layui-btn-xs layui-btn-normal">请选择</span></div>';
                        } else {
                            var tmpName = val.attach_name.split(".")[0]
                            tmpName = tmpName.substring(0, 12);
                            if (tmpName.length > 12) {
                                title += "……"
                            }
                            return '<div>' + val.attach_name + '<span class="layui-btn layui-btn-xs layui-btn-normal horizon-margin">修改</span></div>';
                        }
                    }},
                {field: 'terminal_ids', align: 'left', title: '设置终端', width: 240,event:"terminal_ids",templet:function (val) {
                        if(val.terminal_ids && val.terminal_ids.length){
                            return "已经配置"　+ val.terminal_ids.length +"个终端,点击可修改"
                        }else{
                            return "请点击进行配置"
                        }

                    }},
                {field: 'playingCnt', align: 'center', title: '在播终端数', width: 100,templet:function (val) {
                      if(val.playingCnt==0){
                          return '<span class="layui-btn layui-btn-xs layui-btn-danger">0</span>';
                      }else{
                          return '<span class="layui-btn layui-btn-xs layui-btn-checked">' + val.playingCnt + '</span>';
                      }

                    }},
                {field: 'creator', align: 'center', width: 140,title: '创建人'},
                {field: 'create_time', align: 'center',   width: 180, title: '创建时间', templet: function (val) {
                        return val.create_time.substring(0,19)
                    }
                }

            ]]
        })
    }

    table.on('toolbar(liveplayList)',function (obj) {

        if(obj.event=="delSelect"){
            var postData ={}
            var data = table.checkStatus('liveplayList').data;
            if (data && data.length > 0) {
                var live_ids = []
                for(var i=0;i<data.length;i++){
                    if(data[i].state=="1"||data[i].state=="2"){
                        var showIndex =i +1
                        layer.msg("第" + showIndex+"个任务在等待推流或者已经推流,无法删除!",{icon:0})
                        return
                    }else{
                        live_ids.push(data[i].live_id)
                    }
                }
            }

            layer.confirm('是否真的删除勾选的推流任务?', {icon: 3, title:'提示'}, function(index){
                var postData = {}
                var allCnt =0
                var failCnt =0
                var okCnt =0
                for(var i=0;i<live_ids.length;i++){
                    postData["live_id"] = live_ids[i]
                    server_api.deleteLivePlay(JSON.stringify(postData),function (resp) {
                        allCnt++
                        if(resp.status==0){
                            okCnt++
                        }else{
                            failCnt ++
                        }
                        if(allCnt==live_ids.length){
                            if(okCnt==allCnt){
                                layer.msg("删除成功!",{icon:1})
                            }else if(failCnt >0){
                                layer.msg("有"+failCnt + "个删除失败!",{icon:1})
                            }
                            setCurrentData("yes")
                        }
                    })
                }
            })

        }else if(obj.event=="addNewLivePlay"){
            var postData ={
                name:"新的推流任务名字",
                attach_id:"attach_id123",
                attach_name:"请修改媒体附件!"
            }
            server_api.addOrUpdateLivePlay(JSON.stringify(postData), function (resp) {
                if (resp.status == 0) {
                    layer.msg("新增成功!", {icon: 1});
                    setCurrentData("yes");
                } else {
                    layer.msg("新增失败!", {icon: 2});
                }
            });
        }
    })

    //监听单元格编辑
    table.on('edit(liveplayList)', function (obj) {
        var value = obj.value //得到修改后的值
            , data = obj.data //得到所在行所有键值
            , field = obj.field; //得到字段
        var postData = data
        postData[field] = value

        server_api.addOrUpdateLivePlay(JSON.stringify(postData), function (resp) {
            if (resp.status == 0) {
                layer.msg("修改成功!", {icon: 1});
            } else {
                layer.msg("修改失败!", {icon: 2});
            }
            setCurrentData();

        });
    });

    //监听任务表单项修改
    table.on('tool(liveplayList)', function (obj) {
        var data = obj.data;
        var event = obj.event;
        var msg = ""
        if (event == "start"||event=="stop") {
            if(event=="start"　&& (data.state=="2"||data.state=="1")){
                layer.msg("预案已经启用,请不要重复启用!",{icon:0})
                return
            }else if(event=="stop" && (data.state!="2" && data.state!="1")){
                layer.msg("推流未启用,请先启用!",{icon:0})
                return
            }
            else if(event == "start" && new Date(data.start_time).getTime() < new Date().getTime()){
                // layer.msg("开始推流时间不能小于当前时间!",{icon:0})
                msg ="immediatly"
            }else{
                msg = event
            }
            var postData = {
                live_id:data.live_id,
                action:event
            }
            var confirmTitle ={
                "start":"真的启用此推流播放预案吗?",
                "stop":"真的停止此推流播放吗？",
                "immediatly":"开始推流时间小于当前,将会立即推流!"
            }

            layer.confirm(confirmTitle[msg], function (index) {
                server_api.liveplayStarStop(JSON.stringify(postData),function (resp) {
                    if(resp.status==0){
                        layer.msg("操作成功!",{icon:1})
                        setCurrentData()
                    }else{
                        layer.msg(resp.msg,{icon:2})
                    }
                    RefreshPlayingList()
                })
            });


        } else if (event == "state") {
            if (data.state !="2") {
                layer.msg("还没有开始推流！", {icon: 2})
                return
            }
            var title = "正推实时推流：" + data.name
            var videoUrl = data.rtmpUrl
            var params = "videoUrl=" + videoUrl + "&type=rtmp/flv"
            WeAdminShow(title, '../video/play.html?' + params);
        } else if (event == "attach_name") {
            if((data.state=="2"||data.state=="1")){
                layer.msg("已经启用,不可修改文件!",{icon:0})
                return
            }

            var postData = {
                page: 1,
                pagesize: 9999,
                file_type: data.attach_type,
                getTotal: "yes"
            }
            server_api.getFileList(JSON.stringify(postData),function (resp) {
                if (resp.status == 0) {
                    var allMediaFileList = resp.result;
                    var treeDataList = []
                    for (var i = 0; i < allMediaFileList.length; i++) {
                        var oneFile = {}
                        oneFile.title = allMediaFileList[i].name + "(" + allMediaFileList[i].size + ")"
                        oneFile.id = i
                        oneFile.attach_id = allMediaFileList[i].attach_id
                        oneFile.size = allMediaFileList[i].size
                        oneFile.attach_name = allMediaFileList[i].name
                        treeDataList.push(oneFile)
                    }
                    tree.render({
                        id: 'mediaFileTree'
                        , elem: '#mediaFileTree'
                        , data: treeDataList
                        , isJump: true  //link 为参数匹配
                        , showCheckbox: true
                    });
                    var tmpWinIndex = popWindow("请选择文件", "#setMediaFile", ["50%", "50%"])
                    $("#confirmSelectFile").off("click");
                    $("#confirmSelectFile").on("click", function (object) {
                        var checkedData = tree.getChecked('mediaFileTree'); //获取选中节点的数据

                        if (checkedData.length == 0) {
                            layer.msg("未选择任何文件!", {icon: 2})
                            return
                        } else if (checkedData.length > 1) {
                            layer.msg("只能选择一个文件!", {icon: 2})
                            return
                        }
                        var updateItem = {}
                        updateItem.attach_id = checkedData[0].attach_id
                        updateItem.attach_name = checkedData[0].attach_name
                        obj.update(updateItem)

                        var postData1 = data
                        postData1.attach_id = checkedData[0].attach_id
                        postData1.attach_name = checkedData[0].attach_name

                        server_api.addOrUpdateLivePlay(JSON.stringify(postData1), function (resp) {
                            if (resp.status == 0) {
                                layer.msg("修改成功!", {icon: 1});
                            } else {
                                setCurrentData();
                                layer.msg("修改失败!", {icon: 2});
                            }

                        });

                        layer.close(tmpWinIndex)
                    })
                } else {
                    layer.msg("请现在附件管理里面上传媒体文件!", {icon: 0})
                }
            })

        }else if(event=="mediaTypeChoice"){
            if((data.state=="2"||data.state=="1")){
                layer.msg("已经启用,不可修改类型!",{icon:0})
                return
            }
            var selected = $("#mediaTypeChoice" + data.id).children('option:selected').val()
            if (data.attach_type != selected) {
                var postData = data
                postData.attach_type = selected;
                server_api.addOrUpdateLivePlay(JSON.stringify(postData), function (resp) {
                    if (resp.status == 0) {
                        layer.msg("设置推流类型成功!", {icon: 1})
                        //layer.close(winIndex)
                        var newdata = {};
                        newdata["attach_type"] = selected;
                        obj.update(newdata);
                    } else {
                        layer.msg(resp.msg, {icon: 2})
                    }
                })
            }
        }else if(event=="start_time"){
            if((data.state=="2"||data.state=="1")){
                layer.msg("已经启用,不可修改时间!",{icon:0})
                return
            }else{
                laydate.render({
                    elem: this.firstChild
                    , type: 'datetime'
                    , show: true //直接显示
                    , closeStop: this
                    , done: function (value, date) {
                        var newdata = {};
                        if (!value || event.length == 0) {
                            layer.msg("必须设置时间!");
                            newdata[event] = data[event];
                            obj.update(newdata);
                            return;
                        } else if (value != data.start_date) {
                            var postData = data;
                            postData[event] = value;
                            server_api.addOrUpdateLivePlay(JSON.stringify(postData), function (resp) {
                                if (resp.status == 0) {
                                    layer.msg("修改时间成功!", {icon: 1})
                                    newdata[event] = value;
                                    obj.update(newdata);
                                } else {
                                    layer.msg("修改时间失败!", {icon: 2})
                                }
                            });
                        }
                    }

                });
            }


        } else if(event=="terminal_ids"){

            var postData = {
                page: 1,
                pagesize: 10000,
                getTotal: "yes"
            }

            var oldTerminals = data.terminal_ids


            getTerminalAndGroup()
            var groupList = TerminalGrpList;
            var allTerminalList = AllTerminalList;
            if(groupList==null||allTerminalList==null){
                layer.msg("请先对终端进行分组!",{icon:0})
                getTerminalAndGroup()
                return
            }
            var treeDataList =  common_api.buildTerminalTree(groupList,allTerminalList,oldTerminals,true)
            tree.render({
                id: 'terminalGrpTree'
                , elem: '#terminalGrpTree'
                , data: treeDataList
                , isJump: true  //link 为参数匹配
                , showCheckbox: true
            });

            var tmpIndex = popWindow("请选择终端", "#setTerminals", ["50%", "80%"])
            $("#confirmTerminals").off("click");
            $("#confirmTerminals").on("click", function (object) {
                if((data.state=="2"||data.state=="1")){
                    layer.msg("正在等待推或者推流中,不可设置终端!",{icon:0})
                    return
                }

                var checkedData = tree.getChecked('terminalGrpTree'); //获取选中节点的数据
                var group_ids = []
                var terminal_ids = {}
                var terminalIdList = []

                if (checkedData.length > 0) {
                    for (var h = 0; h < checkedData.length; h++) {
                        if (checkedData[h].children.length > 0) {
                            //var regex = /('(\w+)')/g;
                            for (var i = 0; i < checkedData[h].children.length; i++) {
                                terminal_ids[checkedData[h].children[i].terminal_id] = 1
                                // terminal_ids.push(checkedData[h].children[i].terminal_id)
                            }
                        }
                    }
                    for (var terminalid in terminal_ids) {
                        terminalIdList.push(terminalid)
                    }
                    var postData = data
                    postData.terminal_ids = terminalIdList

                    server_api.addOrUpdateLivePlay(JSON.stringify(postData), function (resp) {
                        if (resp.status == 0) {
                            layer.msg("设置播流终端成功!", {icon: 1})
                            //layer.close(winIndex)
                            var newdata = {}
                            newdata.terminal_ids = terminalIdList;
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
        }

    })

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

    function autoRefresh() {
        setTimeout(function () {
            setCurrentData()
            RefreshPlayingList()
            autoRefresh()
        },30000)
    }


    $(function () {
        RefreshPlayingList()
        getTerminalAndGroup()
        setCurrentData("yes")
        autoRefresh()

    })


})