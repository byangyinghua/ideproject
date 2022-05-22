layui.config({
    base: '../../static/js/'
}).extend({ //设定模块别名
    admin: 'admin',
    server_api: 'server_api'
});

var weeks = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]

var pageData = {
    page: 1,
    pagesize: 10,
    lastPage: 999, //后面计算出来
    total: 0,
    dataList: null
}

var currentUser = null;


layui.use(['jquery', 'form', 'tree', 'table', 'admin', 'laypage', 'laydate', 'server_api'], function () {
    var $ = layui.jquery,
        admin = layui.admin,
        table = layui.table,
        laypage = layui.laypage,
        laydate = layui.laydate,
        form = layui.form,
        tree = layui.tree,
        server_api = layui.server_api;


    function addOrDelItem(action, number) {
        //var newTotal = 0;
        if (action == "add") {
            pageData.total = Number(pageData.total) + Number(number);
        } else if (action == "del") {
            pageData.total = Number(pageData.total) - Number(number);
        }

        var m = pageData.total / pageData.pagesize;
        var n = pageData.total % pageData.pagesize;
        pageData.lastPage = n > 0 ? m + 1 : n;
    }

    function renderPages(total) {
        laypage.render({
            elem: 'pages'
            , count: total
            ,limit:20
            ,limits:[20,30,50,100]
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

        server_api.getUserLoginLog(JSON.stringify(postData), function (resp) {
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

        var colsList = [
            {checkbox: true},
            {field: 'id', align: 'left', title: '序号', width: 80, sort: true},
            {field: 'username', align: 'left', title: '用户名', width: 200, sort: true},
            {field: 'realname', align: 'left', title: '真实姓名', width: 260, sort: true},
            {
                field: 'login_type',
                align: 'left',
                title: '动作',
                width: 140,
                sort: true,
                event: "login_type",
                templet: function (val) {
                    if (val.login_type == "login") {
                        return  '<span class="layui-btn layui-btn-xs layui-btn-normal">登录</span>';
                    } else {
                        return  '<span class="layui-btn layui-btn-xs layui-btn-danger">登出</span>';
                    }
                }
            },
            {field: 'login_ip', align: 'left', title: '登录IP', width: 140, sort: true, event: "login_ip"},
            {field: 'create_time', align: 'left', title: '登录时间', cellMinWidth: 200, sort: true, event: "create_time"},
        ]

        var renderObj = {
            id: "loginLogList",//
            elem: '#loginLogList',//指定表格元素
            data: dataList,  //表格当前页面数据
            // height: 'full-20',
            limit: pageData.pagesize,
            defaultToolbar: ['filter'],
            cellMinWidth: 80, //全局定义常规单元格的最小宽度，layui 2.2.1 新增
            skin: 'line ', //表格风格 line （行边框风格）row （列边框风格）nob （无边框风格）
            done: function (res, curr, count) {
            },
            cols: [colsList]
        }

        table.render(renderObj)
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


    $(function () {
        setCurrentData("yes")
    })


})