# Ability Extensions
You have around 100 abilities in your bot and you're looking for a way to refactor that mess into more modular classes. `AbillityExtension` is here to support just that! It's not a secret that AbilityBot uses refactoring backstage to be able to construct all of your abilities and map them accordingly. However, AbilityBot searches initially for all methods that return an `AbilityExtension` type. Then, those extensions will be used to search for declared abilities. Here's an example.
```java
public class MrGoodGuy implements AbilityExtension {
  public Ability () {
    return Ability.builder()
           .name("nice")
           .privacy(PUBLIC)
           .locality(ALL)
           .action(ctx -> silent.send("You're awesome!", ctx.chatId())
          );
  }
}

public class MrBadGuy implements AbilityExtension {
  public Ability () {
    return Ability.builder()
           .name("notnice")
           .privacy(PUBLIC)
           .locality(ALL)
           .action(ctx -> silent.send("You're horrible!", ctx.chatId())
          );
  }
 }
 
 public class YourAwesomeBot implements AbilityBot {
    
    // Constructor for your bot
  
    public AbilityExtension goodGuy() {
        return new MrGoodGuy();
    }
    
    public AbilityExtension badGuy() {
      return new MrBadGuy();
    }
    
    // Override creatorId
 }
```