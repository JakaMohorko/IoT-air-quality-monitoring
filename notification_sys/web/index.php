<?php
ini_set("error_reporting", E_ALL);
ini_set("display_errors", TRUE);

function get_db(){
    $db = new PDO('sqlite:alert_contacts.db');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    return $db;
}

function in_db($db, $email){
	$check = $db->prepare("SELECT * FROM ADDRESSES where Email=:email");
	$check->bindParam(':email', $email);
	$res = $check->execute();

	if($res){
		$entry = $check->fetch();
		if($entry["Email"] == $email){
			return true;
		}
	}

	return false;
}

function add_to_db($db, $email, $aqiLevel){
	$check = $db->prepare("INSERT INTO ADDRESSES (Email, AqiLevel) VALUES (:email, :aqiLevel);");
	$check->bindParam(':email', $email);
	$check->bindParam(':aqiLevel', $aqiLevel);
	$check->execute();
}


?>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
	<title>Air Quality Edinburgh</title>
</head>

<style type="text/css">
	#jumbo_back{
		/*background-image: url('https://d24ndt2yiijez0.cloudfront.net/uploads/city_page/hero_bg_image/2028/edinburgh-hero-banner.jpg');*/
		background-image: url('https://cdn.dorms.com/city_images/Scotland/edinburgh.jpg');
		background-position: top; 
		background-size: cover;
		height: 100%;
	}
</style>

<body>
	<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  		<!-- Navbar content -->
  		<a class="navbar-brand" href="index.php">Gassie</a>
	  	<button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
	    	<span class="navbar-toggler-icon"></span>
	  	</button>
  		<div class="collapse navbar-collapse" id="navbarNav">
		    <ul class="navbar-nav">
				<li class="nav-item active">
					<a class="nav-link" href="index.php">Home <span class="sr-only">(current)</span></a>
				</li>	
		    </ul>
		</div>
	</nav>


	<div class="jumbotron text-center text-white bg-dark" id="jumbo_back">
  		<h1>Air Quality Edinburgh</h1>
  		<p>Register for Air Quality updates</p>
	</div>


	<?php 
		if (!isset($_POST["email"]) || !isset($_POST["aqiLevel"])) {
	?>
		<div class="container">
			<div class="row">
				<div class="col-lg-12">
					<p> To register for email alerts about air quality from our sensors please enter your email below:</p>
				</div>
			</div>

			<div class="row ">
				<div class="col-lg-12">
					<form action="index.php" method="post">
						<div class="form-group">
							<label for="email" class="" >Email address:</label>
							<!-- <div> -->
						 		<input type="email" class="form-control" id="email" name="email" placeholder="Enter email">
						 	<!-- </div>  -->
						 	<small id="emailHelp" class="form-text text-muted">We'll never share your email with anyone else.</small>
						</div>
						  <div class="form-group">
						    <label for="selectAQIlevel">Select level of AQI to be notified at:</label>
						    <select class="form-control" id="selectAQIlevel" name="aqiLevel" required>
						      <option>Good</option>
						      <option>Moderate</option>
						      <option selected="selected">Unhealthy for Sensitive Groups</option>
						      <option>Unhealthy</option>
						      <option>Very Unhealthy</option>
						      <option>Hazardous</option>
						    </select>
						  </div>
						<!-- <button type="submit" class="btn btn-primary btn-lg btn-block">Submit</button> -->
						<button type="submit" class="btn btn-primary btn-block" name="submited" value="SENT">Submit</button>
					</form>
				</div>
			</div>
		</div>

	<?php
		// var_dump($_POST);

	 	}else{
	 		var_dump($_POST);
	 		// check that it is not empty
	 		if($_POST['email'] == ""){
	 			print("<div class='container'> <h3>Email cannot be empty<h3></div>");
	 			exit;
	 		}

	 		$email = $_POST['email'];
	 		$aqiLevel = $_POST['aqiLevel'];

	 		$db = get_db();


	 		// not needed
	 		if(in_db($db, $email)){
	 			print("<div class='container'> <h3>Email already in database<h3></div>");
	 			exit;
	 		}

	 		add_to_db($db, $email, $aqiLevel);

	 		// say if already registered in database
	 		$email_display= htmlspecialchars($email);
	 		print("<div class='container'> <h3>Email address: ${email_display} is now subscribed<h3> </div>");
	 	}
	 ?>

	<script src="https://code.jquery.com/jquery-3.4.1.slim.min.js" integrity="sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n" crossorigin="anonymous"></script>
	<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
	<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js" integrity="sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6" crossorigin="anonymous"></script>
</body>
</html>