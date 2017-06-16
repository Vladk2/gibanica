var password = document.getElementById("npass");
var old_password = document.getElementById("old_pass");
var regex = new RegExp("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.{8,})");
var button = document.getElementById("saveChanges");

function validatePassword(){
    if (!password.value && !old_password.value){ //ako su polja prazna, onda me ne zanima jer se ne menja
        old_password.setCustomValidity('');
        password.setCustomValidity('');
    }
    else if (!old_password.value) {
        old_password.setCustomValidity('Enter your password');
        password.setCustomValidity('');
    }
    else if(regex.test(password.value)) {
        old_password.setCustomValidity('');
        password.setCustomValidity('');
    }
    else {
        old_password.setCustomValidity('');
        password.setCustomValidity("Minimum length 8, must contain upper and lower case letters and a number");
    }
}

password.onchange = validatePassword;
password.onkeyup = validatePassword;
old_password.onchange = validatePassword;
old_password.onkeyup = validatePassword;