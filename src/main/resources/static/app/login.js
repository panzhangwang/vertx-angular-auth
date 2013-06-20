angular.module('angular-auth-demo').controller({
  LoginController: function ($scope, $http, authService) {
    $scope.submit = function() {
    var data = $.param({username: $scope.username, password: $scope.password});
      $http.post('auth/login', data, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
    }).success(function(data, status) {
	    if (data == 'true'){
			authService.loginConfirmed();
		} else {
			alert('Wrong username or password.');
		}
      });
    }
  }
  
});

