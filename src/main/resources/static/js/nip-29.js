$(function () {
    $("#send10002").click(() => createEvent(generate10002TypeScriptEvent()));
    $("#send39001").click(() => createEvent(generate39001TypeScriptEvent()));
});

function generate10002TypeScriptEvent() {
    const tags = [
        ['r', $("#superconductor_url").val()]
    ];

    return {
        id: '',
        kind: Number($("#10002-kind").val()),
        created_at: Math.floor(Date.now() / 1000),
        content: 'SuperConductor Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}

function generate39001TypeScriptEvent() {
    const tags = [
        ['p', $("#39001-pubkey").val(), $("#afterimage_url").val(), ws]
    ];

    return {
        id: '',
        kind: Number($("#39001-kind").val()),
        created_at: Math.floor(Date.now() / 1000),
        content: 'AfterImage Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}
