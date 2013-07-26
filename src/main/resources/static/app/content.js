angular.module('angular-auth-demo').controller({
  ContentController: function ($scope, $http) {

    $scope.publicContent = [];
    $scope.restrictedContent = [];

    $scope.publicAction = function() {
      var data = $.param({content: $scope.publicData});
      $http.post('apps/protect', data, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
      }).success(function(response) {
        // this piece of code will not be executed until user is authenticated
        $scope.publicContent.push(response);
      });

    }

    $scope.restrictedAction = function() {
      var data = $.param({content: $scope.restrictedData});
      $http.post('apps/protect', data, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
      }).success(function(response) {
        // this piece of code will not be executed until user is authenticated
        $scope.restrictedContent.push(response);
      });
    }

    $scope.logout = function() {
      $http.post('auth/logout').success(function() {
        $scope.restrictedContent = [];
      });
    }

    var eb = new vertx.EventBus('http://localhost:8080/eventbus');
    $scope.messages = [];

    eb.onopen = function() {
      eb.registerHandler('some-address', function(message) {
        console.log('received a message: ' + JSON.stringify(message));        
        $scope.messages.push(message.content);
        $scope.$apply();
      });
    }
    
    $scope.sendMessage = function() {
      $http.get('auth/user').success(function(response) {
        eb.sessionID = response.sessionId;
        eb.publish('some-address', {content: $scope.messageText});        
        $scope.messageText = "";  
      });  
    };

  }
  
});

