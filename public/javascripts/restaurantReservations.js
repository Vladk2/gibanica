/**
 * Created by goran on 23.6.17..
 */

function getReservations() {
    console.log('getReservations');
    var request = new XMLHttpRequest();
    request.open('GET', 'getReservations', true);
    request.send();
    var tableBody = document.getElementById('reservedTableBody');
    tableBody.innerHTML = '<b><i>loading...</i></b>';
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            tableBody.innerHTML = '';
            console.log(request.responseText);
            var html = '';
            var data = JSON.parse(request.responseText);
            for (var i = 0; i < data.length; i++) {
                html = '<tr id="?UserId|?ReservationId">\n ' +
                    '<td>?Name</td> \n' +
                    '<td>?Start</td> \n' +
                    '<td>?Length</td> \n' +
                    '<td>?NoOfSeats</td> \n' +
                    '<td> <button type="button" onclick="cancelReservation(?UserId, ?ReservationId)" style="color: red">Cancel</button></td> \n' +
                    '</tr>\n\n';
                html = html.replace(/\?Name/g, data[i].name).replace(/\?Start/g, timeConverter(data[i].startTimestamp))
                    .replace(/\?Length/g, new Date((data[i].endTimestamp - data[i].startTimestamp)).getHours())
                    .replace(/\?NoOfSeats/g, data[i].seatCount).replace(/\?UserId/g, data[i].userId)
                    .replace(/\?ReservationId/g, data[i].reservationId);
                tableBody.innerHTML += html;
            }
        }
    }
}

function timeConverter(UNIX_timestamp){
    var a = new Date(UNIX_timestamp);
    var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    var year = a.getFullYear();
    var month = months[a.getMonth()];
    var date = a.getDate();
    var hour = a.getHours();
    var min = a.getMinutes();
    var time = date + ' ' + month + ' ' + year + ' ' + (hour > 9 ? hour : "0" + hour) + ':' + (min > 9 ? min : "0" + min);
    return time;
}

function cancelReservation(userId, reservationId) {
    console.log('cancelReservation: ', userId, reservationId);
    var request = new XMLHttpRequest();
    request.open('POST', 'cancelReservation', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: userId, reservationId: reservationId}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            // $('#' + userId + '\|' + reservationId).remove();
            getReservations();
        }
    }
}