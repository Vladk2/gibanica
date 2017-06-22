/**
 * Created by goran on 22.6.17..
 */

function getRestaurantSearchResults(getAll) {
    console.log('getRestaurantSearchResults: ', getAll);
    document.getElementById('restaurantsTableBody').innerHTML = '<i>Please wait...</i>';
    var searchText;
    if (getAll === true) {
        searchText = '';
    }
    else {
        searchText = document.getElementById('searchRestaurantsText').value;
    }
    var request = new XMLHttpRequest();
    request.open('POST', 'searchRestaurants', true);
    request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
    request.send(JSON.stringify({text: searchText}));
    request.onreadystatechange = function () {
        if (request.readyState == 4 && request.status == 200) {
            fillRestaurantsTableBody('restaurantsTableBody', request.responseText);
        }
    }
}

function fillRestaurantsTableBody(tableBodyId, responseText) {
    console.log('fillRestaurantsTableBody: ', tableBodyId, responseText);
    var tableBody = document.getElementById(tableBodyId);
    if (responseText === '[]') {
        tableBody.innerHTML = '<i><b>No results</b></i>';
    }
    else {
        var results = JSON.parse(responseText);
        var tableBodyRowHtml = '';
        var tableBodyHtml = '';
        for(var i = 0; i < results.length; i++) {
            tableBodyRowHtml = '<tr id="row?ID">\n' +
                '<td>' + results[i].name + '</td>\n' +
                '<td>' + results[i].description + '</td>\n' +
                '<td>' + results[i].location + '</td>\n' +
                '<td>' + results[i].tel + '</td>\n' +
                '<td>' + '<button type="button" id="button?ID" onclick="reserve(?ID)">Reserve a table</button> ' + '</td>\n' +
                '</tr>\n\n';
            tableBodyHtml += tableBodyRowHtml.replace(/\?ID/g, results[i].id);
        }
        tableBody.innerHTML = tableBodyHtml;
    }
}

function reserve(id) {
    console.log('reserve: ', id);
    window.location.href = 'reserve/' + id;
}

getRestaurantSearchResults(true);