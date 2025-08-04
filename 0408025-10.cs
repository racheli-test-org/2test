@ -0,0 +1,25 @@
using System.ComponentModel.DataAnnotations;
using System;

namespace WebApplication1;

[Serializable]
public class CreditCardDetails
{
    [Key]
    public string Id { get; set; }

    public string PinCode { get; set; }

    public string Email { get; set; }

    public string DisplayName { get; set; }

    public string CardHolderName { get; set; }

    public string Cvv { get; set; }

    public string CreditCardPayment { get; set; }

    public string ExpirationDate { get; set; }
}
