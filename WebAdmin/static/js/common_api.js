//定义模块
layui.define(["jquery"], function (exports) {

    // var form = layui.form; //只有执行了这一步，部分表单元素才会自动修饰成功
    var $ = layui.$;

    var obj = {
        //一些公用的api terminal全部采用ip
        buildTerminalTree: function (groupList,allTerminalList,selectedTerminals,isShowAll) {
            var treeDataList =[]
            var oneGroup1 = {},tmpMap={}
            var itemtId =0
            oneGroup1.title = "未分组终端"
            oneGroup1.id = itemtId++
            oneGroup1.gid = "ungroup"
            oneGroup1.disabled = true
            oneGroup1.nTerminal = 0
            oneGroup1.offlineCnt =0
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
                oneGroup.offlineCnt =0
                oneGroup.children = []
                tmpMap[groupList[i].gid] = i + 1
                treeDataList.push(oneGroup)
            }
            for (var m = 0; m < allTerminalList.length; m++) {
                var oneTerminal = {}
                var tmpTitle = allTerminalList[m].name
                if(!tmpTitle){
                    tmpTitle = "未命名终端"
                }
                //oneTerminal.title = allTerminalList[m].name + '(' + allTerminalList[m].ip + ')'
                oneTerminal.id = itemtId++
                oneTerminal.terminal_id = allTerminalList[m].terminal_id
                oneTerminal.ip = allTerminalList[m].ip
                if (selectedTerminals) {
                    for (var i = 0; i < selectedTerminals.length; i++) {
                        if (selectedTerminals[i] == oneTerminal.terminal_id) {
                            oneTerminal.checked = true
                            break
                        }
                    }
                }
                if (allTerminalList[m].gids && allTerminalList[m].gids.length > 0) {
                    for (var w = 0; w < allTerminalList[m].gids.length; w++) {
                        if(allTerminalList[m].state==0){
                            //treeDataList[tmpMap[allTerminalList[m].gids[w]]].disabled = true
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].offlineCnt++
                            //oneTerminal.disabled = true
                            oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + '|未在线)'
                        }else{
                            oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + ')'
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].disabled = false
                        }

                        if(isShowAll){
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].children.push(oneTerminal)
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].nTerminal++
                        }else if(oneTerminal.checked){
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].children.push(oneTerminal)
                            treeDataList[tmpMap[allTerminalList[m].gids[w]]].nTerminal++
                        }
                    }
                } else {
                    if(allTerminalList[m].state==0){
                        treeDataList[0].disabled = true
                        treeDataList[0].offlineCnt++
                        //oneTerminal.disabled = true
                        oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + '|未在线)'
                    }else{
                        treeDataList[0].disabled = false
                        oneTerminal.title = tmpTitle + '(' + allTerminalList[m].ip + ')'
                    }

                    if(isShowAll){
                        treeDataList[0].children.push(oneTerminal)
                        treeDataList[0].nTerminal++
                    }else if(oneTerminal.checked){
                        treeDataList[0].children.push(oneTerminal)
                        treeDataList[0].nTerminal++
                    }

                }
            }
            for(var x=0;x<treeDataList.length;x++){
                if(treeDataList[x].offlineCnt >0){
                    treeDataList[x].title = treeDataList[x].title + '(未在线:' + treeDataList[x].offlineCnt + '个)'
                }
                if(treeDataList[x].children.length ==0){
                    treeDataList[x].disabled = true
                }
            }

            return treeDataList
        },

        getCheckTermialIPs:function (checkedData) {
            var terminal_ips = {}
            var terminalIpList = []
            if (checkedData.length > 0) {
                for (var h = 0; h < checkedData.length; h++) {
                    if (checkedData[h].children.length > 0) {
                        //var regex = /('(\w+)')/g;
                        for (var i = 0; i < checkedData[h].children.length; i++) {
                            terminal_ips[checkedData[h].children[i].ip] = 1
                        }
                    }

                }
                for (var terminal_ip in terminal_ips) {
                    terminalIpList.push(terminal_ip)
                }
            }

            return terminalIpList
        },
        getCheckTermialIds:function (checkedData) {
            var terminal_ids = {}
            var terminalIdList = []
            if (checkedData.length > 0) {
                for (var h = 0; h < checkedData.length; h++) {
                    if (checkedData[h].children.length > 0) {
                        //var regex = /('(\w+)')/g;
                        for (var i = 0; i < checkedData[h].children.length; i++) {
                            terminal_ids[checkedData[h].children[i].terminal_id] = 1
                        }
                    }

                }
                for (var terminal_id in terminal_ids) {
                    terminalIdList.push(terminal_id)
                }
            }
            return terminalIdList
        },

        getToDayDate:function(){
            var date = new Date(); //创建时间对象
            var year = date.getFullYear(); //获取年
            var month = date.getMonth()+1;//获取月
            var day = date.getDate(); //获取当日
            var today = year+"-"+month+"-"+day; //组合时间
            return today
        },
        timestampToTime:function(timestamp) {
            var date = new Date(timestamp);//时间戳为10位需*1000，时间戳为13位的话不需乘1000
            var Y = date.getFullYear() + '-';
            var M = (date.getMonth()+1 < 10 ? '0'+(date.getMonth()+1) : date.getMonth()+1) + '-';
            var D = (date.getDate() < 10 ? '0'+date.getDate() : date.getDate()) + ' ';
            var h = (date.getHours() < 10 ? '0'+date.getHours() : date.getHours()) + ':';
            var m = (date.getMinutes() < 10 ? '0'+date.getMinutes() : date.getMinutes()) + ':';
            var s = (date.getSeconds() < 10 ? '0'+date.getSeconds() : date.getSeconds());

            let strDate = Y+M+D+h+m+s;
            return strDate;
        },
        diffWithToday:function(oldDate){
            var theOld= new Date(oldDate.replace("-","/") + " 23:59:59").getTime()
            var today = new Date().getTime()
            return theOld -today
        },
        timeCalculateMinute:function(newtime,oldtime){
            var theNew = new Date("2020/01/01 "+newtime + ":00").getTime()
            var theOld= new Date("2020/01/01 "+oldtime + ":00").getTime()
            return theNew - theOld
        },
        timeAddValue:function(oldtime,value){

            var theOld = new Date("2020/01/01 "+oldtime + ":00").getTime()
            theOld += value

            var mytime=this.timestampToTime(theOld)
            console.log("oldtime:",oldtime,mytime)
            return mytime.split(" ")[1].substr(0,5)
        },
        simpleCopyObject(theObj){
            var objTmp = JSON.stringify(theObj)
            return JSON.parse(objTmp)
        }

    }

    //输出模块
    exports('common_api', obj);
});