<?php
// Prüfen, ob die E-Mail-Variable angekommen ist
if (isset($_POST['email'])) {
    
    $userEmail = $_POST['email'];
    
    // Sicherheits-Check: Ist es eine echte E-Mail?
    if (filter_var($userEmail, FILTER_VALIDATE_EMAIL)) {
        
        // Der Name der Textdatei, in der die E-Mails gespeichert werden
        $dateiName = "emails.txt";
        
        // Die E-Mail-Adresse für die Datei vorbereiten (fügt einen Zeilenumbruch hinzu)
        $eintrag = $userEmail . PHP_EOL;
        
        // In die Datei schreiben:
        // FILE_APPEND sorgt dafür, dass neue E-Mails unten angehängt werden, statt alte zu überschreiben.
        // LOCK_EX verhindert, dass zwei Nutzer gleichzeitig in die Datei schreiben und Fehler erzeugen.
        //file_put_contents($dateiName, $eintrag, FILE_APPEND | LOCK_EX);
		file_put_contents($dateiName, $eintrag, LOCK_EX);
        // 2. Stat-Cache zwingend leeren!
		clearstatcache();	
        // Rückmeldung an JavaScript
        echo "Vielen Dank! Die E-Mail-Adresse wurde erfolgreich gespeichert.";
        
    } else {
        echo "Fehler: Ungültiges E-Mail-Format.";
    }
} else {
    echo "Fehler: Keine Daten empfangen.";
}
?>
