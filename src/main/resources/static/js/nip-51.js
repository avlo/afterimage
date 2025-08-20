$(function () {
    $("#send10007").click(() => createEvent(generate10007NostrEvent()));
    $("#send39001").click(() => createEvent(generate39001NostrEvent()));
});

function generate10007NostrEvent() {
    const tags = [
        ['relay', $("#superconductor_url").val()]
    ];

    return {
        id: '',
        kind: Number($("#10007-kind").val()),
        created_at: new Date().getMilliseconds(),
        content: 'SuperConductor Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}

function generate39001NostrEvent() {
    const tags = [
        ['p', $("#39001-pubkey").val(), $("#afterimage_url").val(), ws]
    ];

    return {
        id: '',
        kind: Number($("#39001-kind").val()),
        created_at: new Date().getMilliseconds(),
        content: 'AfterImage Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}
