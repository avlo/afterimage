$(function () {
    $("#send39002").click(() => createEvent(generate39002TypeScriptEvent()));
    $("#send39001").click(() => createEvent(generate39001TypeScriptEvent()));
});

function generate39002TypeScriptEvent() {
    const tags = [
        ['p', $("#39002-pubkey").val(), $("#superconductor_url").val()]
    ];

    return {
        id: '',
        kind: Number($("#39002-kind").val()),
        created_at: Math.floor(Date.now() / 1000),
        content: 'SuperConductor Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}

function generate39001TypeScriptEvent() {
    const tags = [
        ['p', $("#39001-pubkey").val(), $("#afterimage_url").val()]
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
