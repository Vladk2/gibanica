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

/*
function validateSearch() {
    if (nameTextBox.value === '' && surnameTextBox.value === '') {
        console.log('bad')
        nameTextBox.setCustomValidity('Fill out at least one field');
        surnameTextBox.setCustomValidity('');
    }
    else {
        console.log('good')
        nameTextBox.setCustomValidity('');
        surnameTextBox.setCustomValidity('');
    }

}
 nameTextBox.onchange = validateSearch;
 nameTextBox.onkeydown = validateSearch;
 surnameTextBox.onchange = validateSearch;
 surnameTextBox.onkeydown = validateSearch;
*/

function fetchSearchResults() {
    document.getElementById('searchDiv').style.display = 'block';
    console.log('fetching results');
    var tableBody = document.getElementById('searchResultsTableBody');
    tableBody.innerHTML = 'loading...';
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (request.readyState == 4 && request.status == 200) {
            fillTableBodyWithResults('searchResultsTableBody', request.responseText, 'search');
        }
    };
    request.open('POST', 'friendsSearch', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({name: nameTextBox.value, surname: surnameTextBox.value}));
}

function fillTableBodyWithResults(tableBodyId, resultsText, buttonIdPrefix) {
    var tableBodyDom = document.getElementById(tableBodyId);
    var tableBodyHtml = '\n';
    console.log(resultsText);
    var resultObject = JSON.parse(resultsText);
    console.log(toString(resultObject));
    for (var i = 0; i < resultObject.length; i++) {
        tableBodyHtml += '<tr>\n' +
                '<td>' + resultObject[i].name + '</td>\n' +
                '<td>' + resultObject[i].surname + '</td>\n';
        //sad treba odluciti koje dugme
        var onclickCall = '';
        var buttonText = '';
        var additionalButton = '';
        if (resultObject[i].accepted === true) {
            onclickCall = 'removeCancelFriend(ID?, \''+ buttonIdPrefix +'\')';
            buttonText = 'Delete';
        }
        else if (resultObject[i].sent === true && resultObject[i].received === false) {
            onclickCall = 'removeCancelFriend(ID?, \''+ buttonIdPrefix +'\')';
            buttonText = 'Cancel';
        }
        else if (resultObject[i].sent === false && resultObject[i].received === true) {
            onclickCall = 'rejectFriend(ID?, \''+ buttonIdPrefix +'\')';
            buttonText = 'Reject';
            additionalButton = '<button id="' + buttonIdPrefix + 'Acceptbutton' + resultObject[i].userId + '" type="button" ' +
                'onclick="acceptFriend(ID?, \''+ buttonIdPrefix +'\')">Accept</button>';
        }
        else if (resultObject[i].sent === false && resultObject[i].received === false) {
            onclickCall = 'addFriend(ID?, \''+ buttonIdPrefix +'\')';
            buttonText = 'Add';
        }
        onclickCall = onclickCall.replace('ID?', resultObject[i].userId);
        additionalButton = additionalButton.replace('ID?', resultObject[i].userId);
        tableBodyHtml += '<td><button id="' + buttonIdPrefix + 'button' + resultObject[i].userId + '" type="button" onclick="' +
            onclickCall + '">'+ buttonText +'</button> ' +
            additionalButton + '</td>\n</tr>\n\n';
    }
    tableBodyDom.innerHTML = tableBodyHtml;
}

function addFriend(id, buttonIdPrefix) {
    console.log('addFriend: ', id);
    var buttonDom = document.getElementById(buttonIdPrefix + 'button' + id);
    var request = new XMLHttpRequest();
    request.open('POST', 'addFriend', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            buttonDom.setAttribute('onclick', 'removeCancelFriend(' + id + ', \''+ buttonIdPrefix +'\')');
            buttonDom.innerHTML = 'Cancel';
            fetchFriends();
            fetchPending();
        }
    }
}

function removeCancelFriend(id, buttonIdPrefix) {
    console.log('removeCancelFriend: ', id);
    var buttonDom = document.getElementById(buttonIdPrefix + 'button' + id);
    console.log(buttonIdPrefix + 'button' + id);
    var request = new XMLHttpRequest();
    request.open('POST', 'deleteFriend', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            buttonDom.setAttribute('onclick', 'addFriend(' + id + ', \''+ buttonIdPrefix +'\')');
            buttonDom.innerHTML = 'Add';
            fetchFriends();
            fetchPending();
        }
    }
}

function rejectFriend(id, buttonIdPrefix) {
    console.log('rejectFriend: ', id);
    var buttonTdParentDom = document.getElementById(buttonIdPrefix + 'button' + id).parentNode;
    var request = new XMLHttpRequest();
    request.open('POST', 'deleteFriend', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            var foo = '<button type="button" id="?buttonIdPrefix?button?id"' +
                'onclick="addFriend(?id, \'?buttonIdPrefix\')">Add</button>';
            foo = foo.replace(/\?id/g, id).replace(/\?buttonIdPrefix/g, buttonIdPrefix);
            console.log(foo);
            buttonTdParentDom.innerHTML = foo;
            fetchFriends();
            fetchPending();
        }
    }
}

function acceptFriend(id, buttonIdPrefix) {
    console.log('acceptFriend: ', id);
    var buttonTdParentDom = document.getElementById(buttonIdPrefix + 'button' + id).parentNode;
    var request = new XMLHttpRequest();
    request.open('POST', 'acceptFriend', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({userId: id}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            var foo = '<button type="button" id="?buttonIdPrefix?button?id"' +
                'onclick="removeCancelFriend(?id, \'?buttonIdPrefix\')">Delete</button>';
            foo = foo.replace(/\?id/g, id).replace(/\?buttonIdPrefix/g, buttonIdPrefix);
            buttonTdParentDom.innerHTML = foo;
            fetchFriends();
            fetchPending();
        }
    }
}

function fetchFriends() {
    console.log('fetching friends');
    var request = new XMLHttpRequest();
    request.open('GET', 'friendsGetAll', true);
    request.send();
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            fillTableBodyWithResults('friendsTableBody', request.responseText, 'friends');
            if (request.responseText === '[]') {
                document.getElementById('friendsDiv').style.display = 'none';
            }
            else {
                document.getElementById('friendsDiv').style.display = 'block';
            }
        }
    }
}

function fetchPending() {
    console.log('fetching pending');
    var request = new XMLHttpRequest();
    request.open('GET', 'friendsGetPending', true);
    request.send();
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            fillTableBodyWithResults('pendingTableBody', request.responseText, 'pending');
            if (request.responseText === '[]') {
                document.getElementById('pendingDiv').style.display = 'none';
            }
            else {
                document.getElementById('pendingDiv').style.display = 'block';
            }
        }
    }
}

