function test() {
    var x = [1, 2, 3];
    var $tmp = x[0];
    x[0]++;
    return $tmp;
}

function box() {
    if (test() != 1) return "fail";
    return "OK"
}