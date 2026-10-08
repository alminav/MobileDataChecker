<?php
// 1. PHPMailer-Klassen manuell einbinden (Pfade ggf. anpassen)
require 'phpmailer/Exception.php';
require 'phpmailer/PHPMailer.php';
require 'phpmailer/SMTP.php';

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

$mail = new PHPMailer(true);

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
    // --- EMPFÄNGER & ABSENDER ---
    $mail->setFrom('noreply@almica.bplaced.net', 'almica'); // Absender
    $mail->addAddress('alt.micha@gmail.com');                         // Empfänger hinzufügen
	// $mail->addAttachment('whatsapp_micha.png', 'almica'); 
    // --- INHALT ---
    $mail->isHTML(true);                                        // Als HTML-E-Mail senden
    $mail->Subject = 'Test E-Mail via PHPMailer';               // Betreff
    $mail->Body    = '<h1>Hallo!</h1><p>Das ist eine sichere HTML-Nachricht.</p>'; // Inhalt
    $mail->AltBody = 'Hallo! Das ist eine sichere Text-Nachricht.'; // Text-Fallback

    // Senden
    $mail->send();
    echo 'Nachricht wurde erfolgreich versendet.';
} catch (Exception $e) {
    echo "Nachricht konnte nicht gesendet werden. Mailer-Fehler: {$mail->ErrorInfo}";
}
?>
