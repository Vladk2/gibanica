/**
 * Created by goran on 20.6.17..
 */

/*<input id="hideSearchDivButton" type="button" value="X" style="position:absolute; top:0; right:0;" onclick="document.getElementById('searchDiv').setAttribute('style', 'display:none');">*/

var nameTextBox = document.getElementById('searchTextName'),
    surnameTextBox = document.getElementById('searchTextSurname');


//prvo funkcija za prikaz liste prijatelja
function hideShowElement(id) {
    var element = document.getElementById(id);
    element.style.display = element.style.display === 'none' ? 'block' : 'none';
}

function fillTableBodyWithResults(tableBodyId, resultsText, buttonIdPrefix) {
    var tableBodyDom = document.getElementById(tableBodyId);
    var tableBodyHtml = '\n';
    console.log(resultsText);
    var resultObject = JSON.parse(resultsText);
    console.log(toString(resultObject));
    for (var i = 0; i < resultObject.length; i++) {
        tableBodyHtml +=
            '<tr>\n' +
                '<td>' + resultObject[i].name + '</td>\n' +
                '<td>' + resultObject[i].surname + '</td>\n' +
                '<td><button type="button" onclick="inviteFriend(' + resultObject[i].userId + ')" ' +
                    'id="button' + resultObject[i].userId + '">Invite</button> </td>\n' +
            '</tr>';
    }
    tableBodyDom.innerHTML = tableBodyHtml;
}

function fetchFriends() {
    console.log('fetching friends');
    var request = new XMLHttpRequest();
    request.open('GET', 'friendsGetAll', true);
    request.send();
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            fillTableBodyWithResults('friendsTableBody', request.responseText, 'friends');
            // if (request.responseText === '[]') {
            //     document.getElementById('friendsDiv').style.display = 'none';
            // }
            // else {
            //     document.getElementById('friendsDiv').style.display = 'block';
            // }
        }
    }
}

function inviteFriend(id) {
    console.log('inviteFriend: ', id);
    var buttonDom = document.getElementById('button' + id);
    console.log('button' + id);
    var request = new XMLHttpRequest();
    request.open('POST', 'inviteFriend', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id,
        reservationId: $('#reservationIdInput').val(),
        restaurantId: $('#restaurantIdInput').val()
    }));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            buttonDom.setAttribute('onclick', 'cancelInvite(' + id + ')');
            buttonDom.innerHTML = 'Cancel';
        }
    }

}

function cancelInvite(id) {
    console.log('inviteFriend: ', id);
    var buttonDom = document.getElementById('button' + id);
    console.log('button' + id);
    var request = new XMLHttpRequest();
    request.open('POST', 'cancelInvite', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id,
        reservationId: $('#reservationIdInput').val(),
        restaurantId: $('#restaurantIdInput').val()
    }));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            buttonDom.setAttribute('onclick', 'cancelFriend(' + id + ')');
            buttonDom.innerHTML = 'Invite';
        }
    }

}



