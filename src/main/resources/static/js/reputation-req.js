$(function () {
    $("#sendrequest").click(() => sendContentRequest());
    $("#reqclose").click(() => sendClose());
});

function cullEmptyKeyValuePairs() {
    return {
        // 'ids': (($("#idcontent").val() !== "") ? [$("#idcontent").val()] : undefined),
        // 'authors': (($("#authors1").val() !== "") ? [$("#authors1").val()] : undefined)
        'kinds': [8],
        '#p': [$("#referencePubKeys").val()],
        '#d': ['REPUTATION']
        // 'since': clickNow-1000,
        // 'until': clickNow,
        // 'limit': '1'
    }
}

function stringifyJson() {
    return JSON.stringify(cullEmptyKeyValuePairs());
}

function populateRequestJson() {
    return "["
        + "\"REQ\","
        + "\"" + $("#subscription_id").val() + "\","
        + stringifyJson()
        + "]";
}

function sendContentRequest() {
    console.log("\nsending content...\n\n");
    let outboundJson = populateRequestJson();
    console.log(outboundJson);
    console.log('\n\n');
    ws.send(outboundJson);
}

function sendClose() {
    let closeJson = populateCloseJson();
    console.log(closeJson);
    console.log('\n\n');
    ws.send(closeJson);
    setConnected(false);
    console.log("Disconnected via Nostr CLOSE");
}

function populateCloseJson() {
    return "["
        + "\"CLOSE\","
        + "\"" + $("#subscription_id").val() + "\""
        + "]";
}
