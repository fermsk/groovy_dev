class ATMExample {
    static void main(String[] args) {
        int balance = 100000, cash, deposit
        Scanner sc = new Scanner(System.in)
        while (true) {
            println "ATM emulate"
            println "Choose 1 to take money"
            println "Choose 2 to put money"
            println "Choose 3 to check balance"
            println "Choose 4 for exit"
            print "Enter number of operation: "

            int choice = sc.nextInt()
            switch (choice) {
                case 1:
                    print "Input sum you want to get: "
                    cash = sc.nextInt()
                    if (balance >= cash) {
                        balance -= cash
                        println "Please, take your money"
                    } else {
                        println "No enough money on the balance"
                    }
                    println ""
                    break

                case 2:
                    print "Input sum to put: "
                    deposit = sc.nextInt()
                    balance += deposit
                    println "Sum $deposit inputed succesfully"
                    println ""
                    break

                case 3:
                    println "Balance : $balance"
                    println ""
                    break

                case 4:
                    System.exit(0)
            }
        }
    }
}