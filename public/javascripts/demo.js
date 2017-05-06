/**
 * Created by stefan on 5/6/17.
 */
var events = [
    {'Date': new Date(2017, 4, 1), 'Title': 'Doctor appointment at 3:25pm.'},
    {'Date': new Date(2017, 4, 18), 'Title': 'New Garfield movie comes out!', 'Link': 'https://garfield.com'},
    {'Date': new Date(2017, 4, 27), 'Title': '25 year anniversary', 'Link': 'https://www.google.com.au/#q=anniversary+gifts'},
];
var settings = {};
var element = document.getElementById('calendar');
calendar(element, events, settings);
