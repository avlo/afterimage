$(function () {
    $("#send10007").click(() => createEvent(generate10007NostrEvent()));
    $("#send30002").click(() => createEvent(generate30002NostrEvent()));
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

function generate30002NostrEvent() {
    const tags = [
        ['relay', $("#afterimage_url").val()]
    ];

    return {
        id: '',
        kind: Number($("#30002-kind").val()),
        created_at: new Date().getMilliseconds(),
        content: 'AfterImage Relay Sets Event',
        tags: tags,
        pubkey: '',
        sig: ''
    };
}
