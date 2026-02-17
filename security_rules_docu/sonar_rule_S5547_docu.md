**Cipher algorithms should be robust**  

[Strong cipher algorithms](https://en.wikipedia.org/wiki/Strong_cryptography) are cryptographic systems resistant to cryptanalysis, they are not vulnerable to well-known attacks like brute force attacks for example.

A general recommendation is to only use cipher algorithms intensively tested and promoted by the cryptographic community.

More specifically for block cipher, it’s not recommended to use algorithm with a block size inferior than 128 bits.

Noncompliant Code Example
    
    
    import javax.crypto.Cipher;
    import java.security.NoSuchAlgorithmException;
    import javax.crypto.NoSuchPaddingException;
    
    public class test {
    
        public static void main(String[] args) {
          try
          {
            Cipher c1 = Cipher.getInstance("DES"); // Noncompliant: DES works with 56-bit keys allow attacks via exhaustive search
            Cipher c7 = Cipher.getInstance("DESede"); // Noncompliant: Triple DES is vulnerable to meet-in-the-middle attack
            Cipher c13 = Cipher.getInstance("RC2"); // Noncompliant: RC2 is vulnerable to a related-key attack
            Cipher c19 = Cipher.getInstance("RC4"); // Noncompliant: vulnerable to several attacks (see https://en.wikipedia.org/wiki/RC4#Security)
            Cipher c25 = Cipher.getInstance("Blowfish"); // Noncompliant: Blowfish use a 64-bit block size makes it vulnerable to birthday attacks
    
            NullCipher nc = new NullCipher(); // Noncompliant: the NullCipher class provides an "identity cipher" one that does not transform or encrypt the plaintext in any way.
          }
          catch(NoSuchAlgorithmException|NoSuchPaddingException e)
          {
          }
        }
    }
    

Compliant Solution
    
    
    import javax.crypto.Cipher;
    import java.security.NoSuchAlgorithmException;
    import javax.crypto.NoSuchPaddingException;
    
    public class test {
    
        public static void main(String[] args) {
          try
          {
            Cipher c31 = Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
          }
          catch(NoSuchAlgorithmException|NoSuchPaddingException e)
          {
          }
        }
    }
    

See

  * [OWASP Top 10 2021 Category A2](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/) \- Cryptographic Failures 
  * [OWASP Top 10 2017 Category A3](https://www.owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure) \- Sensitive Data Exposure 
  * [Mobile AppSec Verification Standard](https://mobile-security.gitbook.io/masvs/security-requirements/0x08-v3-cryptography_verification_requirements) \- Cryptography Requirements 
  * [OWASP Mobile Top 10 2016 Category M5](https://owasp.org/www-project-mobile-top-10/2016-risks/m5-insufficient-cryptography) \- Insufficient Cryptography 
  * [MITRE, CWE-327](https://cwe.mitre.org/data/definitions/327) \- Use of a Broken or Risky Cryptographic Algorithm 
  * [CERT, MSC61-J.](https://wiki.sei.cmu.edu/confluence/x/hDdGBQ) \- Do not use insecure or weak cryptographic algorithms 
  * [SANS Top 25](https://www.sans.org/top25-software-errors/#cat3) \- Porous Defenses 