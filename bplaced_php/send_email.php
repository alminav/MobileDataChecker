<?php
// 1. PHPMailer-Klassen manuell einbinden (Pfade ggf. anpassen)
require 'phpmailer/Exception.php';
require 'phpmailer/PHPMailer.php';
require 'phpmailer/SMTP.php';

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

$mail = new PHPMailer(true);
//echo "Empfangen: Lat = " . var_export($_GET['latitude'], true) . " | Lon = " . var_export($_GET['longitude'], true);

try {
    // --- SERVER EINSTELLUNGEN ---
    // Wenn Sie bplaced pro/max nutzen, schalten Sie SMTP ein:
    $mail->isMail();                                            // Über SMTP senden

    /* 
    HINWEIS FÜR BPLACED FREE: 
    Falls Sie den kostenlosen Tarif nutzen, schlägt SMTP fehl. 
    Kommentieren Sie die oberen SMTP-Zeilen aus und nutzen Sie stattdessen:
    $mail->isMail(); 
    */

	// 1. Parameter auslesen und absichern
	// basename() sorgt dafür, dass nur der reine Dateiname (z.B. "rechnung.pdf") übrig bleibt 
	// und keine Pfad-Manipulationen (wie ../) möglich sind.
	$dateiname = isset($_GET['datei']) ? basename($_GET['datei']) : '';
	// FILTER_SANITIZE_EMAIL entfernt ungültige Zeichen aus der E-Mail-Adresse (ohne basename)
	$mailto = isset($_GET['mailto']) ? filter_var($_GET['mailto'], FILTER_SANITIZE_EMAIL) : '';
	$latitude = filter_input(INPUT_GET, 'latitude', FILTER_VALIDATE_FLOAT);
	$longitude = filter_input(INPUT_GET, 'longitude', FILTER_VALIDATE_FLOAT);

	// 2. Den vollständigen Pfad auf deinem bplaced-Webspace zusammensetzen
	$ordnerPfad = 'uploads/'; // Der Ordner, in dem deine Dateien liegen
	$vollerPfad = $ordnerPfad . $dateiname;

	// 3. Prüfen, ob die Datei existiert und der Parameter nicht leer ist
	if (!empty($dateiname) && file_exists($vollerPfad)) {		
		// Parameter 1: Der echte Pfad auf dem Server
		// Parameter 2: Der Name, den der Empfänger in der Mail sieht
		$mail->addAttachment($vollerPfad, 'almica');
		
	} else {
		die("Fehler: Datei existiert nicht oder kein Dateiname angegeben.");
	}
	if (empty($mailto)) {		
		$mailto = 'alt.micha@gmail.com';
	}

	// Offizielles Google-Maps-Format für Apps und Browser gleichermaßen
	// KORREKT: Der String wurde sauber getrennt und die URL-Parameter richtig angehängt
	// Neu: Fängt Fehler UND leere Aufrufe ab
	if ($latitude === false || $latitude === null || $longitude === false || $longitude === null) {
		//die("invalid coordinates");
		php_console_log("invalid coordinates");
		$googleMapsUrl = '';
	} else {
		php_console_log("coordinates ok ${latitude} ${longitude}");
		$googleMapsUrl = 'https://maps.google.com/?q=' . $latitude . ',' . $longitude;
	}

	
	// Ein schicker, responsiver HTML-Button für die E-Mail
	if ($latitude && $longitude) {
	$body = '
		<div style="font-family: Arial, sans-serif; padding: 20px; color: #333; background-color: #f9f9f9; border-radius: 8px;">
			<h2 style="color: #007bff; margin-top: 0;">Hallo Micha!</h2>
			<p style="font-size: 16px; line-height: 1.5;">Es wurde ein neues Bild hochgeladen. Der Standort wurde erfasst und steht dir hier zur Verfügung:</p>
			
			<div style="margin: 25px 0;">
				<!-- Der Google Maps Button -->
				<a href="' . $googleMapsUrl . '" target="_blank" style="background-color: #28a745; color: white; padding: 12px 24px; text-decoration: none; font-weight: bold; border-radius: 4px; display: inline-block; box-shadow: 0 2px 5px rgba(0,0,0,0.15);">
					📍 Standort in Google Maps öffnen
				</a>
			</div>
			
			<p style="font-size: 12px; color: #777; margin-top: 30px; border-top: 1px solid #ddd; padding-top: 10px;">
				Datei: ' . htmlspecialchars($dateiname) . '<br>
				Koordinaten: ' . $latitude . ', ' . $longitude . '
			</p>
		</div>';
	} else {
	$body = '
		<div style="font-family: Arial, sans-serif; padding: 20px; color: #333; background-color: #f9f9f9; border-radius: 8px;">

			<p style="font-size: 12px; color: #777; margin-top: 30px; border-top: 1px solid #ddd; padding-top: 10px;">
				Datei: ' . htmlspecialchars($dateiname) . '
			</p>
		</div>';
	}
    // --- EMPFÄNGER & ABSENDER ---
    $mail->setFrom('noreply@almica.bplaced.net', 'almica');
    $mail->addAddress($mailto);

    // --- INHALT ---
    $mail->isHTML(true);
    $mail->Subject = 'Neuer Standort-Upload von almica';
    $mail->Body    = $body;
    $mail->AltBody = "Hallo! Ein neues Bild wurde hochgeladen. Standort: " . $googleMapsUrl;

    // Senden
    $mail->send();
    echo "Nachricht wurde erfolgreich versendet {$mailto}.";
    
    // --- 2. AUFRÄUMEN (DATEI LÖSCHEN) ---
    // Nachdem die Mail erfolgreich versendet wurde, löschen wir das Bild vom Server
	/*
    if (file_exists($vollerPfad)) {
        unlink($vollerPfad);
    }
	*/
	} catch (Exception $e) {
		echo "Nachricht konnte nicht gesendet werden. Mailer-Fehler: {$mail->ErrorInfo}";
	}

	function php_console_log($daten) {
		// Wandelt PHP-Arrays oder Objekte in gültiges JavaScript-Format (JSON) um
		$json_daten = json_encode($daten);
		
		// Gibt den JavaScript-Code direkt im Browser aus
		echo "<script>console.log('PHP-Log: ' + " . $json_daten . ");</script>";
	}
?>
