function resetNode(id) {
    const path = `/rest/nodes/action?id=${id}&action=reset`;
    BaseItem._send(path);
}