$(function () {
    $("#send02").click(() => createEvent(generate02TypeScriptEvent()));
});

function generate02TypeScriptEvent() {
    const tags = [
        ['p', $("#00-pubkey").val(), $("#superconductor_url").val()]
    ];

    let event = {
        id: '',
        kind: Number($("#03-kind").val()),
        created_at: Math.floor(Date.now() / 1000),
        content: 'SuperConductor Follows List Event',
        tags: tags,
        pubkey: '',
        sig: ''
    }
    return event;
}
